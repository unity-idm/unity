/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.server.*;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.LogoutMode;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.webui.authn.LogoutProcessorsManager;
import pl.edu.icm.unity.webui.authn.WebLogoutHandler;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;

/**
 * Handles logouts in Vaadin 23 way
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Vaddin23WebLogoutHandler implements WebLogoutHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, Vaddin23WebLogoutHandler.class);
	private static final String LOGOUT_REDIRECT_TRIGGERING = Vaddin23WebLogoutHandler.class.getName() +
			".invokeLogout";
	private static final String LOGOUT_REDIRECT_RET_URI = Vaddin23WebLogoutHandler.class.getName() +
			".returnUri";

	private final UnityServerConfiguration config;
	private final SessionManagement sessionMan;
	private final LogoutProcessorsManager logoutProcessorsManager;
	private final RememberMeProcessor rememberMeProcessor;

	public Vaddin23WebLogoutHandler(UnityServerConfiguration config, SessionManagement sessionMan,
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
		Page p = UI.getCurrent().getPage();
		String contextPath = VaadinServlet.getCurrent().getServletContext().getContextPath();
		logoutSessionPeers(URI.create(contextPath), soft);
		p.reload();
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
	
	public static UnsuccessfulAuthenticationCounter getLoginCounter()
	{
		HttpSession httpSession = ((WrappedHttpSession)VaadinSession.getCurrent().getSession()).getHttpSession();
		return (UnsuccessfulAuthenticationCounter) httpSession.getServletContext().getAttribute(
				UnsuccessfulAuthenticationCounter.class.getName());
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
