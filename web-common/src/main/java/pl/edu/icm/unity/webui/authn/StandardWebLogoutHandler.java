/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Primary;
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

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;

/**
 * Handles logouts in Vaadin way
 */
@Component
@Primary
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StandardWebLogoutHandler implements WebLogoutHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, StandardWebLogoutHandler.class);
	private static final String LOGOUT_REDIRECT_TRIGGERING = StandardWebLogoutHandler.class.getName() + 
			".invokeLogout";
	private static final String LOGOUT_REDIRECT_RET_URI = StandardWebLogoutHandler.class.getName() + 
			".returnUri";
	
	private final UnityServerConfiguration config;
	private final SessionManagement sessionMan;
	private final LogoutProcessorsManager logoutProcessorsManager;
	private final RememberMeProcessor rememberMeProcessor;
	
	public StandardWebLogoutHandler(UnityServerConfiguration config, SessionManagement sessionMan,
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
		Page p = Page.getCurrent();
		logoutSessionPeers(p.getLocation(), soft);
		p.reload();
	}

	@Override
	public void logout(boolean soft, String logoutRedirectPath)
	{
		throw new NotImplementedException("This class is deprecated and will be removed. Use: VaddinWebLogoutHandler");
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
			vSession.setAttribute(LOGOUT_REDIRECT_RET_URI,
					Page.getCurrent().getLocation().toASCIIString());
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
