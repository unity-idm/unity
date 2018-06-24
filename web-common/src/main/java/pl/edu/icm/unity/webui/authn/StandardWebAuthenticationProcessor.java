/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.Page;
import com.vaadin.server.SynchronizedRequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.RememberMeInfo;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.LogoutMode;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.session.SessionParticipants;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.authn.RemeberMeHelper.RememberMeCookie;

/**
 * Handles results of authentication and if it is all right, redirects to the source application.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StandardWebAuthenticationProcessor implements WebAuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandardWebAuthenticationProcessor.class);
	private static final String LOGOUT_REDIRECT_TRIGGERING = StandardWebAuthenticationProcessor.class.getName() + 
			".invokeLogout";
	private static final String LOGOUT_REDIRECT_RET_URI = StandardWebAuthenticationProcessor.class.getName() + 
			".returnUri";
	
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private SessionParticipantTypesRegistry participantTypesRegistry;
	@Autowired
	private SessionManagement sessionMan;
	@Autowired
	private LoginToHttpSessionBinder sessionBinder;
	@Autowired
	private LogoutProcessorsManager logoutProcessorsManager;
	@Autowired
	private AuthenticationProcessor authnProcessor;
	@Autowired
	private EntityManagement entityMan;
	@Autowired
	private ExecutorsService executorsService;
	@Autowired
	private RemeberMeHelper rememberMeHelper;
	
	
	@Override
	public PartialAuthnState processPrimaryAuthnResult(AuthenticationResult result, String clientIp, 
			AuthenticationRealm realm,
			AuthenticationFlow authenticationFlow, boolean rememberMe, String authnOptionId) throws AuthenticationException
	{
		UnsuccessfulAuthenticationCounter counter = getLoginCounter();
		PartialAuthnState authnState;
		try
		{
			authnState = authnProcessor.processPrimaryAuthnResult(result, authenticationFlow, authnOptionId);
		} catch (AuthenticationException e)
		{
			if (!(e instanceof UnknownRemoteUserException))
				counter.unsuccessfulAttempt(clientIp);
			throw e;
		}

		Optional<RememberMeCookie> rememberMeCookie = rememberMeHelper
				.getRememberMeUnityCookie(VaadinServletRequest.getCurrent(),
						realm.getName());
		boolean skipSecondFactor = false;
		if (authnState.isSecondaryAuthenticationRequired())
		{
			skipSecondFactor = isSecondFactorIsRemembered(rememberMeCookie, realm,
					clientIp);
			if (!skipSecondFactor)
				return authnState;
		}

		AuthenticatedEntity logInfo = authnProcessor
				.finalizeAfterPrimaryAuthentication(authnState, skipSecondFactor);
		if (skipSecondFactor)
		{
			rememberMeHelper.updateRememberMeCookieAndUnityToken(rememberMeCookie.get(),
					realm, VaadinServletResponse.getCurrent());
		}

		logged(logInfo, realm, rememberMe && !skipSecondFactor,
				AuthenticationProcessor.extractParticipants(result),
				skipSecondFactor, authnOptionId);

		finalizeLogin(logInfo);
		return null;
	}

	private boolean isSecondFactorIsRemembered(Optional<RememberMeCookie> rememberMeCookie, AuthenticationRealm realm, String clientIp)
	{
		if (rememberMeCookie.isPresent())
		{
			Optional<RemeberMeToken> rememberMeToken;
			try
			{
				rememberMeToken = rememberMeHelper
						.getAndCheckRememberMeUnityToken(rememberMeCookie.get(),
								realm);
			} catch (AuthenticationException e)
			{
				UnsuccessfulAuthenticationCounter counter = getLoginCounter();
				counter.unsuccessfulAttempt(clientIp);
				rememberMeHelper.clearRememberMeCookieAndUnityToken(realm.getName(),
						VaadinServletRequest.getCurrent(),
						VaadinServletResponse.getCurrent());
				return false;
			}
					
			if (rememberMeToken.isPresent())
			{
				if (!rememberMeToken.get().getRememberMePolicy()
						.equals(RememberMePolicy.disallow))
				{
					return true;
				}
			} else
			{
				rememberMeHelper.clearRememberMeCookieAndUnityToken(realm.getName(),
						VaadinServletRequest.getCurrent(),
						VaadinServletResponse.getCurrent());
			}

		}
		return false;
	}
	
	@Override
	public void processSecondaryAuthnResult(PartialAuthnState state, AuthenticationResult result2, String clientIp, 
			AuthenticationRealm realm,
			AuthenticationFlow authenticationFlow, boolean rememberMe) throws AuthenticationException
	{
		UnsuccessfulAuthenticationCounter counter = getLoginCounter();
		AuthenticatedEntity logInfo;
		try
		{
			logInfo = authnProcessor.finalizeAfterSecondaryAuthentication(state, result2);
		} catch (AuthenticationException e)
		{
			if (!(e instanceof UnknownRemoteUserException))
				counter.unsuccessfulAttempt(clientIp);
			throw e;
		}

		logged(logInfo, realm, rememberMe, 
				AuthenticationProcessor.extractParticipants(state.getPrimaryResult(), result2), false, state.getAuthnOptionId());

		finalizeLogin(logInfo);
	}
	
	private void finalizeLogin(AuthenticatedEntity logInfo) throws AuthenticationException
	{
		if (logInfo.getOutdatedCredentialId() != null)
		{
			//simply reload - we ensure that session reinit after login won't outdate session
			//authN handler anyway won't let us in to the target endpoint with outdated credential
			//and we will get outdated credential dialog from the AuthnUI
			UI ui = UI.getCurrent();
			ui.getPage().reload();
			return;
		}
		
		gotoOrigin(executorsService.getService());
	}
	
	private String getLabel(long entityId)
	{
		try
		{
			return entityMan.getEntityLabel(new EntityParam(entityId));
		} catch (AuthorizationException e)
		{
			log.debug("Not setting entity's label as the client is not authorized to read the attribute", e);
		} catch (EngineException e)
		{
			log.error("Can not get the attribute designated with EntityName", e);
		}
		return null;
	}
	
	private void logged(AuthenticatedEntity authenticatedEntity,
			final AuthenticationRealm realm, final boolean rememberMe,
			List<SessionParticipant> participants, boolean skippedSecondFactor,
			String auhtnOptionId) throws AuthenticationException
	{
		long entityId = authenticatedEntity.getEntityId();
		String label = getLabel(entityId);
		final LoginSession ls = sessionMan.getCreateSession(entityId, realm, 
				label, authenticatedEntity.getOutdatedCredentialId(), 
				null, new RememberMeInfo(false, skippedSecondFactor), auhtnOptionId);
		InvocationContext.getCurrent().setLoginSession(ls);
		try
		{
			sessionMan.updateSessionAttributes(ls.getId(), 
					new SessionParticipants.AddParticipantToSessionTask(
							participantTypesRegistry,
							participants.toArray(new SessionParticipant[participants.size()])));
		} catch (IllegalArgumentException e)
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
		
		if (rememberMe)
			rememberMeHelper.setRememberMeCookie(realm, entityId, ls.getStarted(), auhtnOptionId);
		
		ls.addAuthenticatedIdentities(authenticatedEntity.getAuthenticatedWith());
		ls.setRemoteIdP(authenticatedEntity.getRemoteIdP());
		if (ls.isUsedOutdatedCredential())
			log.debug("User {} logged with outdated credential", ls.getEntityId());
	}
	
	private static void gotoOrigin(ScheduledExecutorService executor) throws AuthenticationException
	{
		UI ui = UI.getCurrent();
		if (ui == null)
		{
			log.error("BUG Can't get UI to redirect the authenticated user.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		
		executor.schedule(() -> ui.getSession().close(), 10, TimeUnit.SECONDS);
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
	
	@Override
	public void logout()
	{
		logout(false);
	}

	@Override
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
		} catch (IllegalArgumentException e)
		{
			log.warn("Can not refresh the state of the current session. Logout of session participants "
					+ "won't be performed", e);
			destroySession(soft);
			return;
		}
		
		if (mode == LogoutMode.internalAndSyncPeers)
		{
			logoutProcessorsManager.handleSynchronousLogout(session);
			destroySession(soft);
		} else
		{
			VaadinSession vSession = VaadinSession.getCurrent(); 
			vSession.addRequestHandler(new LogoutRedirectHandler());
			vSession.setAttribute(LOGOUT_REDIRECT_TRIGGERING, new Boolean(soft));
			vSession.setAttribute(LOGOUT_REDIRECT_RET_URI, Page.getCurrent().getLocation().toASCIIString());
		}
		
		rememberMeHelper.clearRememberMeCookieAndUnityToken(session.getRealm(),
				VaadinServletRequest.getCurrent(),
				VaadinServletResponse.getCurrent());

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
				} catch (IllegalArgumentException e)
				{
					log.warn("Can not refresh the state of the current session. "
							+ "Logout of session participants won't be performed", e);
					destroySession(softLogout);
					return false;
				}

				try
				{
					logoutProcessorsManager.handleAsyncLogout(loginSession, null, 
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
