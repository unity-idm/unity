/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

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
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationProcessorUtil;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.UIBgThread;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.ui.UI;

/**
 * Handles results of authentication and if it is all right, redirects to the source application.
 * 
 * TODO - this is far from being complete: needs to support fragments.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationProcessor.class);
	public static final String UNITY_SESSION_COOKIE_PFX = "USESSIONID_";
	
	
	private UnityMessageSource msg;
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private AttributesInternalProcessing attrProcessor;
	private CredentialEditorRegistry credEditorReg;
	private SessionManagement sessionMan;
	private LoginToHttpSessionBinder sessionBinder;
	
	@Autowired
	public AuthenticationProcessor(UnityMessageSource msg, AuthenticationManagement authnMan,
			SessionManagement sessionMan, LoginToHttpSessionBinder sessionBinder,
			IdentitiesManagement idsMan, AttributesInternalProcessing attrMan,
			CredentialEditorRegistry credEditorReg)
	{
		this.msg = msg;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.attrProcessor = attrMan;
		this.credEditorReg = credEditorReg;
		this.sessionMan = sessionMan;
		this.sessionBinder = sessionBinder;
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
		
		logged(logInfo, realm, rememberMe);

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
			final boolean rememberMe) throws AuthenticationException
	{
		long entityId = authenticatedEntity.getEntityId();
		String label = getLabel(entityId);
		Date absoluteExpiration = (realm.getAllowForRememberMeDays() > 0 && rememberMe) ? 
				new Date(System.currentTimeMillis()+getAbsoluteSessionTTL(realm)) : null;
		final LoginSession ls = sessionMan.getCreateSession(entityId, realm, 
				label, authenticatedEntity.isUsedOutdatedCredential(), 
				absoluteExpiration);
		VaadinSession vss = VaadinSession.getCurrent();
		if (vss == null)
		{
			log.error("BUG: Can't get VaadinSession to store authenticated user's data.");
			throw new AuthenticationException("AuthenticationProcessor.authnInternalError");
		}
		
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
		Page p = Page.getCurrent();
		URI currentLocation = p.getLocation();
		//FIXME - workaround for the Vaadin bug http://dev.vaadin.com/ticket/12346
		// when the bug is fixed session should be invalidated in the current thread.
		// the workaround is ugly - it is possible that the 'logout' thread completes after we return answer
		// -> then user is logged out but no effect is visible automatically.
		UIBgThread logoutWorker = new UIBgThread()
		{
			@Override
			public void safeRun()
			{
				destroySession(false);
			}
		};
		Thread logoutThread = new Thread(logoutWorker);
		logoutThread.setPriority(Thread.MAX_PRIORITY);
		logoutThread.start();
		p.setLocation(currentLocation);
	}

	public void logout(boolean soft)
	{
		Page p = Page.getCurrent();
		destroySession(soft);
		p.reload();
	}
	
	public static UnsuccessfulAuthenticationCounter getLoginCounter()
	{
		HttpSession httpSession = ((WrappedHttpSession)VaadinSession.getCurrent().getSession()).getHttpSession();
		return (UnsuccessfulAuthenticationCounter) httpSession.getServletContext().getAttribute(
				UnsuccessfulAuthenticationCounter.class.getName());
	}
}
