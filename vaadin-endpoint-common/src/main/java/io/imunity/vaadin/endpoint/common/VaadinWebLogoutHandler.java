/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.LogoutMode;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.CookieHelper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handles logouts in Vaadin 24 way
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class VaadinWebLogoutHandler implements WebLogoutHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, VaadinWebLogoutHandler.class);
	private static final String LOGOUT_REDIRECT_TRIGGERING = VaadinWebLogoutHandler.class.getName() +
			".invokeLogout";
	private static final String LOGOUT_REDIRECT_RET_URI = VaadinWebLogoutHandler.class.getName() +
			".returnUri";

	private final UnityServerConfiguration config;
	private final SessionManagement sessionMan;
	private final LogoutProcessorsManager logoutProcessorsManager;
	private final RememberMeProcessor rememberMeProcessor;

	public VaadinWebLogoutHandler(UnityServerConfiguration config, SessionManagement sessionMan,
	                              LogoutProcessorsManager logoutProcessorsManager, RememberMeProcessor rememberMeProcessor)
	{
		this.config = config;
		this.sessionMan = sessionMan;
		this.logoutProcessorsManager = logoutProcessorsManager;
		this.rememberMeProcessor = rememberMeProcessor;
	}

	private void destroySession(boolean soft)
	{
		InvocationContext invocationContext = InvocationContext.getCurrent();
		LoginSession ls = invocationContext.getLoginSession();
		if (ls != null)
		{
			clearSessionCookie(ls.getRealm());
			sessionMan.removeSession(ls.getId(), soft);
		} else
			throw new IllegalStateException("There is no login session");
	}

	private void clearSessionCookie(String realmName)
	{
		VaadinResponse response = VaadinResponse.getCurrent();
		if (response == null)
			return;
		Cookie cookie = CookieHelper.setupHttpCookie(SessionCookie.getSessionCookieName(realmName), "", 0);
		response.addCookie(cookie);
	}
	
	@Override
	public void logout()
	{
		logout(false);
	}

	@Override
	public void logout(boolean soft)
	{
		UI.getCurrent().getPage().fetchCurrentURL(url ->
				{
					try
					{
						URI pageURI = url.toURI();
						logoutSessionPeers(pageURI, soft);
						UI.getCurrent()
								.getPage()
								.setLocation(pageURI);

					} catch (URISyntaxException e)
					{
						log.error("Logout failed", e);
					}
				});
	}

	@Override
	public void logout(boolean soft, String logoutRedirectPath)
	{
		String contextPath = VaadinServlet.getCurrent().getServletContext().getContextPath();
		if(!logoutRedirectPath.endsWith("/"))
			logoutRedirectPath += "/";
		
		URI redirectURI= URI.create(contextPath + logoutRedirectPath);
		logoutSessionPeers(redirectURI, soft);
		UI.getCurrent().getPage().setLocation(redirectURI);
	}

	private void logoutSessionPeers(URI currentLocation, boolean soft)
	{
		LogoutMode mode = config.getEnumValue(UnityServerConfiguration.LOGOUT_MODE,
				LogoutMode.class);

		LoginSession contextSession = InvocationContext.getCurrent().getLoginSession();

		if (mode == LogoutMode.internalOnly)
		{
			destroySession(soft);
		} else if (mode == LogoutMode.internalAndSyncPeers)
		{
			try
			{
				LoginSession session = sessionMan.getSession(contextSession.getId());
				logoutProcessorsManager.handleSynchronousLogout(session);
			} catch (IllegalArgumentException e)
			{
				log.warn("Can not refresh the state of the current session. Logout of session participants "
						+ "won't be performed", e);
			}
			destroySession(soft);		
		} else
		{
			VaadinSession vSession = VaadinSession.getCurrent();
			vSession.addRequestHandler(new LogoutRedirectHandler());
			vSession.setAttribute(LOGOUT_REDIRECT_TRIGGERING, soft);
			vSession.setAttribute(LOGOUT_REDIRECT_RET_URI, currentLocation.getPath());
		}
		
		// clear remember me cookie and token only when whole authn is remembered
		rememberMeProcessor.removeRememberMeWithWholeAuthn(contextSession.getRealm(),
				VaadinServletRequest.getCurrent(),
				VaadinServletResponse.getCurrent());

	}

	public static UnsuccessfulAccessCounter getLoginCounter()
	{
		HttpSession httpSession = ((WrappedHttpSession) VaadinSession.getCurrent().getSession()).getHttpSession();
		return (UnsuccessfulAccessCounter) httpSession.getServletContext().getAttribute(
				UnsuccessfulAccessCounter.class.getName());
	}

	private class LogoutRedirectHandler extends SynchronizedRequestHandler
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
				} catch (Exception e)
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
