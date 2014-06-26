/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.vaadin.shared.ApplicationConstants;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.server.utils.CookieHelper;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Servlet filter forwarding unauthenticated requests to the protected authentication servlet.
 * @author K. Benedyczak
 */
public class AuthenticationFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationFilter.class);

	private String protectedServletPath;
	private String authnServletPath;
	private final String sessionCookie;
	private UnsuccessfulAuthenticationCounter dosGauard;
	private SessionManagement sessionMan;
	private LoginToHttpSessionBinder sessionBinder;
	
	
	public AuthenticationFilter(String protectedServletPath, String authnServletPath, AuthenticationRealm realm,
			SessionManagement sessionMan, LoginToHttpSessionBinder sessionBinder)
	{
		this.protectedServletPath = protectedServletPath;
		this.authnServletPath = authnServletPath;
		dosGauard = new UnsuccessfulAuthenticationCounter(realm.getBlockAfterUnsuccessfulLogins(), 
				realm.getBlockFor()*1000);
		sessionCookie = AuthenticationProcessor.getSessionCookieName(realm.getName());
		this.sessionMan = sessionMan;
		this.sessionBinder = sessionBinder;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		String servletPath = httpRequest.getServletPath();
		if (hasPathPrefix(servletPath, authnServletPath))
		{
			log.debug("Direct access to authentication servlet is prohibited");
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, 
					"The requested address is not available.");
			return;
		}
		
		if (!hasPathPrefix(servletPath, protectedServletPath))
		{
			gotoNotProtectedResource(httpRequest, response, chain);
			return;
		}
		
		HttpSession httpSession = httpRequest.getSession(false);
		String loginSessionId;
		if (httpSession != null)
		{
			LoginSession loginSession = (LoginSession) httpSession.getAttribute(
					LoginToHttpSessionBinder.USER_SESSION_KEY);
			if (loginSession != null)
			{
				loginSessionId = loginSession.getId();
				try
				{
					if (!hasPathPrefix(httpRequest.getPathInfo(), 
							ApplicationConstants.HEARTBEAT_PATH + '/'))
					{
						log.trace("Update session activity for " + loginSessionId);
						sessionMan.updateSessionActivity(loginSessionId);
					}
					gotoProtectedResource(httpRequest, response, chain);
					return;
				} catch (WrongArgumentException e)
				{
					log.debug("Can't update session activity ts for " + loginSessionId + 
							" - expired(?), HTTP session " + httpSession.getId(), e);
				}
			}
		}

		loginSessionId = CookieHelper.getCookie(httpRequest, sessionCookie);
		
		if (loginSessionId == null)
		{
			forwardtoAuthn(httpRequest, httpResponse);
			return;
		}
		
		String clientIp = request.getRemoteAddr();
		long blockedTime = dosGauard.getRemainingBlockedTime(clientIp); 
		if (blockedTime > 0)
		{
			log.debug("Blocked potential DoS/brute force authN attack from " + clientIp);
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access is blocked for " + 
					TimeUnit.MILLISECONDS.toSeconds(blockedTime) + 
					"s more, due to sending too many invalid session cookies.");
			return;
		}
		
		LoginSession ls;
		try
		{
			ls = sessionMan.getSession(loginSessionId);
		} catch (WrongArgumentException e)
		{
			log.trace("Got request with invalid login session id " + loginSessionId + " to " +
					httpRequest.getRequestURI() );
			dosGauard.unsuccessfulAttempt(clientIp);
			clearSessionCookie(httpResponse);
			forwardtoAuthn(httpRequest, httpResponse);
			return;
		}
		dosGauard.successfulAttempt(clientIp);
		if (httpSession == null)
			httpSession = httpRequest.getSession(true);

		sessionBinder.bindHttpSession(httpSession, ls);
		
		gotoProtectedResource(httpRequest, response, chain);
	}

	private void forwardtoAuthn(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException, ServletException
	{
		String forwardURI = authnServletPath;
		if (httpRequest.getPathInfo() != null) 
		{
			forwardURI += httpRequest.getPathInfo();
		}
		if (log.isTraceEnabled()) 
		{
			log.trace("Request to protected address, forward: " + 
					httpRequest.getRequestURI() + " -> " + httpRequest.getContextPath() + forwardURI);
		}
		RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(forwardURI);
		dispatcher.forward(httpRequest, response);
	}

	private void gotoProtectedResource(HttpServletRequest httpRequest, ServletResponse response, FilterChain chain) 
			throws IOException, ServletException
	{
		if (log.isTraceEnabled())
			log.trace("Request to protected address, user is authenticated: " + 
					httpRequest.getRequestURI());
		chain.doFilter(httpRequest, response);
	}
	
	private void gotoNotProtectedResource(HttpServletRequest httpRequest,
			ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		if (log.isTraceEnabled())
			log.trace("Request to not protected address: " + httpRequest.getRequestURI());
		chain.doFilter(httpRequest, response);
	}
	
	private void clearSessionCookie(HttpServletResponse response)
	{
		Cookie unitySessionCookie = new Cookie(sessionCookie, "");
		unitySessionCookie.setPath("/");
		unitySessionCookie.setSecure(true);
		unitySessionCookie.setMaxAge(0);
		response.addCookie(unitySessionCookie);
	}
	
	public static boolean hasPathPrefix(String pathInfo , String prefix) 
	{
		if (pathInfo == null || pathInfo.equals("")) {
			return false;
		}

		if (!prefix.startsWith("/")) {
			prefix = '/' + prefix;
		}

		if (pathInfo.startsWith(prefix)) {
			return true;
		}

		return false;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}
}
