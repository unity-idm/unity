/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.api.internal.SessionParticipants;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationProcessorUtil;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.authn.LogoutProcessor;
import pl.edu.icm.unity.server.authn.LogoutProcessorFactory;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.registries.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration.LogoutMode;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.vaadin.server.Page;
import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.ui.UI;

/**
 * Handles results of authentication and if it is all right, redirects to the source application.
 * 
 * TODO - we should support fragments here.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationProcessor.class);
	public static final String UNITY_SESSION_COOKIE_PFX = "USESSIONID_";
	private static final String LOGOUT_REDIRECT_TRIGGERING = AuthenticationProcessor.class.getName() + 
			".invokeLogout";
	private static final String LOGOUT_REDIRECT_RET_URI = AuthenticationProcessor.class.getName() + 
			".returnUri";
	
	private UnityMessageSource msg;
	private UnityServerConfiguration config;
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private AttributesInternalProcessing attrProcessor;
	private CredentialEditorRegistry credEditorReg;
	private SessionParticipantTypesRegistry participantTypesRegistry;
	private SessionManagement sessionMan;
	private LoginToHttpSessionBinder sessionBinder;
	private LogoutProcessor logoutProcessor;
	
	@Autowired
	public AuthenticationProcessor(UnityMessageSource msg, AuthenticationManagement authnMan,
			SessionManagement sessionMan, LoginToHttpSessionBinder sessionBinder,
			IdentitiesManagement idsMan, AttributesInternalProcessing attrMan,
			CredentialEditorRegistry credEditorReg, LogoutProcessorFactory logoutProcessorFactory,
			UnityServerConfiguration config, SessionParticipantTypesRegistry participantTypesRegistry)
	{
		this.msg = msg;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.attrProcessor = attrMan;
		this.credEditorReg = credEditorReg;
		this.sessionMan = sessionMan;
		this.sessionBinder = sessionBinder;
		this.config = config;
		this.participantTypesRegistry = participantTypesRegistry;
		this.logoutProcessor = logoutProcessorFactory.getInstance();
	}

	public void processResults(List<AuthenticationResult> results, String clientIp, AuthenticationRealm realm,
			boolean rememberMe) throws AuthenticationException
	{
		UnsuccessfulAuthenticationCounter counter = getLoginCounter();
		AuthenticatedEntity logInfo;
		try
		{
			logInfo = AuthenticationProcessorUtil.processResults(results);
		} catch (AuthenticationException e)
		{
			if (!(e instanceof UnknownRemoteUserException))
				counter.unsuccessfulAttempt(clientIp);
			throw e;
		}
		
		logged(logInfo, realm, rememberMe, AuthenticationProcessorUtil.extractParticipants(results));

		if (logInfo.isUsedOutdatedCredential())
		{
			showCredentialUpdate();
			return;
		}
		gotoOrigin();
	}

	private String getLabel(long entityId)
	{
		try
		{
			AttributeExt<?> attr = attrProcessor.getAttributeByMetadata(
					new EntityParam(entityId), "/", 
					EntityNameMetadataProvider.NAME);
			return (attr != null) ? (String) attr.getValues().get(0) : null;
		} catch (AuthorizationException e)
		{
			log.debug("Not setting entity's label as the client is not authorized to read the attribute", e);
		} catch (EngineException e)
		{
			log.error("Can not get the attribute designated with EntityName", e);
		}
		return null;
	}
	
	private void showCredentialUpdate()
	{
		OutdatedCredentialDialog dialog = new OutdatedCredentialDialog(msg, authnMan, idsMan, credEditorReg,
				this);
		dialog.show();
	}
	
	private void logged(AuthenticatedEntity authenticatedEntity, final AuthenticationRealm realm, 
			final boolean rememberMe, List<SessionParticipant> participants) throws AuthenticationException
	{
		long entityId = authenticatedEntity.getEntityId();
		String label = getLabel(entityId);
		Date absoluteExpiration = (realm.getAllowForRememberMeDays() > 0 && rememberMe) ? 
				new Date(System.currentTimeMillis()+getAbsoluteSessionTTL(realm)) : null;
		final LoginSession ls = sessionMan.getCreateSession(entityId, realm, 
				label, authenticatedEntity.isUsedOutdatedCredential(), 
				absoluteExpiration);
		InvocationContext.getCurrent().setLoginSession(ls);
		try
		{
			sessionMan.updateSessionAttributes(ls.getId(), 
					new SessionParticipants.AddParticipantToSessionTask(
							participantTypesRegistry,
							participants.toArray(new SessionParticipant[participants.size()])));
		} catch (WrongArgumentException e)
		{
			log.error("Can't store session participants", e);
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
		{
			log.error("BUG: Can't get VaadinSession to store authenticated user's data.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		
		//prevent session fixation
		VaadinService.reinitializeSession(VaadinService.getCurrentRequest());
		
		final HttpSession httpSession = ((WrappedHttpSession) vss.getSession()).getHttpSession();
	
		sessionBinder.bindHttpSession(httpSession, ls);
		
		VaadinServletResponse servletResponse = (VaadinServletResponse) VaadinService.getCurrentResponse();
		setupSessionCookie(getSessionCookieName(realm.getName()), ls.getId(), servletResponse, rememberMe, realm);

		InvocationContext.getCurrent().addAuthenticatedIdentities(authenticatedEntity.getAuthenticatedWith());
	}
	
	public static String getSessionCookieName(String realmName)
	{
		return UNITY_SESSION_COOKIE_PFX+realmName;
	}
	
	private static int getAbsoluteSessionTTL(AuthenticationRealm realm)
	{
		return 3600*24*realm.getAllowForRememberMeDays();
	}
	

	
	
	private static void setupSessionCookie(String cookieName, String sessionId, 
			HttpServletResponse servletResponse, boolean rememberMe, AuthenticationRealm realm)
	{
		Cookie unitySessionCookie = new Cookie(cookieName, sessionId);
		unitySessionCookie.setPath("/");
		unitySessionCookie.setSecure(true);
		unitySessionCookie.setHttpOnly(true);
		if (rememberMe && realm.getAllowForRememberMeDays() > 0)
		{
			unitySessionCookie.setMaxAge(getAbsoluteSessionTTL(realm));
		}
		servletResponse.addCookie(unitySessionCookie);
	}
	
	private static void gotoOrigin() throws AuthenticationException
	{
		UI ui = UI.getCurrent();
		if (ui == null)
		{
			log.error("BUG Can't get UI to redirect the authenticated user.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		ui.getSession().close();
		ui.getPage().reload();
	}
	
	private void destroySession(boolean soft)
	{
		InvocationContext invocationContext = InvocationContext.getCurrent();
		LoginSession ls = invocationContext.getLoginSession();
		if (ls != null)
			sessionMan.removeSession(ls.getId(), soft);
		else
			throw new IllegalStateException("There is no login session");
	}
	
	public void logout()
	{
		logout(false);
	}

	public void logout(boolean soft)
	{
		Page p = Page.getCurrent();
		logoutSessionPeers(p.getLocation(), soft);
		p.reload();
	}
	
	private void logoutSessionPeers(URI currentLocation, boolean soft)
	{
		LogoutMode mode = config.getEnumValue(UnityServerConfiguration.LOGOUT_MODE, LogoutMode.class);
		
		if (mode == LogoutMode.internalOnly)
		{
			destroySession(soft);
			return;
		}
		
		LoginSession session = InvocationContext.getCurrent().getLoginSession();
		try
		{
			session = sessionMan.getSession(session.getId());
		} catch (WrongArgumentException e)
		{
			log.warn("Can not refresh the state of the current session. Logout of session participants "
					+ "won't be performed", e);
			destroySession(soft);
			return;
		}
		
		if (mode == LogoutMode.internalAndSyncPeers)
		{
			logoutProcessor.handleSynchronousLogout(session);
			destroySession(soft);
		} else
		{
			VaadinSession vSession = VaadinSession.getCurrent(); 
			vSession.addRequestHandler(new LogoutRedirectHandler());
			vSession.setAttribute(LOGOUT_REDIRECT_TRIGGERING, new Boolean(soft));
			vSession.setAttribute(LOGOUT_REDIRECT_RET_URI, Page.getCurrent().getLocation().toASCIIString());
		}
	}
	
	public static UnsuccessfulAuthenticationCounter getLoginCounter()
	{
		HttpSession httpSession = ((WrappedHttpSession)VaadinSession.getCurrent().getSession()).getHttpSession();
		return (UnsuccessfulAuthenticationCounter) httpSession.getServletContext().getAttribute(
				UnsuccessfulAuthenticationCounter.class.getName());
	}
	
	public class LogoutRedirectHandler extends SynchronizedRequestHandler
	{
		@Override
		public boolean synchronizedHandleRequest(VaadinSession session,
				VaadinRequest request, VaadinResponse responseO) throws IOException
		{
			Boolean softLogout = (Boolean) session.getAttribute(LOGOUT_REDIRECT_TRIGGERING); 
			if (softLogout != null)
			{
				String returnUri = (String) session.getAttribute(LOGOUT_REDIRECT_RET_URI); 
				session.removeRequestHandler(this);
				VaadinServletResponse response = (VaadinServletResponse) responseO;
				LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
				try
				{
					loginSession = sessionMan.getSession(loginSession.getId());
				} catch (WrongArgumentException e)
				{
					log.warn("Can not refresh the state of the current session. "
							+ "Logout of session participants won't be performed", e);
					destroySession(softLogout);
					return false;
				}

				try
				{
					logoutProcessor.handleAsyncLogout(loginSession, null, 
							returnUri, 
							response.getHttpServletResponse());
				} catch (IOException e)
				{
					log.warn("Logout of session peers failed", e);
				}
				destroySession(softLogout);
				return true;
			}
			return false;
		}
	}
}
