/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
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
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.WebSession;

/**
 * Servlet filter redirecting unauthenticated requests to the protected addresses,
 * to the authentication servlet.
 * @author K. Benedyczak
 */
public class AuthenticationFilter implements Filter
{
	public static final String ORIGINAL_ADDRESS = AuthenticationFilter.class.getName()+".origURIkey";
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationFilter.class);

	private String protectedPath;
	private String authnServletPath;
	private final String sessionCookie;
	private UnsuccessfulAuthenticationCounter dosGauard;
	private SessionManagement sessionMan;
	private LoginToHttpSessionBinder sessionBinder;
	
	
	public AuthenticationFilter(String protectedPath, String authnServletPath, String realmName,
			SessionManagement sessionMan, LoginToHttpSessionBinder sessionBinder)
	{
		this.protectedPath = protectedPath;
		this.authnServletPath = authnServletPath;
		dosGauard = new UnsuccessfulAuthenticationCounter(20, 3*60000);
		sessionCookie = AuthenticationProcessor.getSessionCookieName(realmName);
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
		if (!hasPathPrefix(servletPath, protectedPath))
		{
			if (log.isTraceEnabled())
				log.trace("Request to not protected address: " + httpRequest.getRequestURI());
			chain.doFilter(httpRequest, response);
			return;
		}

		HttpSession session = httpRequest.getSession(false);
		String sessionId = getUnitySessionIdFromCookie(httpRequest);
		
		if (session != null)
		{
			LoginSession ls = (LoginSession) session.getAttribute(WebSession.USER_SESSION_KEY);
			if (ls != null)
			{
				try
				{
					if (!hasPathPrefix(httpRequest.getPathInfo(), 
							ApplicationConstants.HEARTBEAT_PATH + '/'))
					{
						log.trace("Update session activity for " + sessionId);
						sessionMan.updateSessionActivity(sessionId);
					}
				} catch (WrongArgumentException e)
				{
					log.debug("Can't update session activity ts for " + sessionId + 
							" - expired(?)");
				}
				gotoResource(httpRequest, response, chain);
				return;
			}
		}

		if (sessionId == null)
		{
			gotoAuthn(httpRequest, httpResponse);
			return;
		}
		
		String clientIp = request.getRemoteAddr();
		long blockedTime = dosGauard.getRemainingBlockedTime(clientIp); 
		if (blockedTime > 0)
		{
			log.debug("Blocked potential DoS/brute force authN attack from " + clientIp);
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access is blocked for " + 
					blockedTime/1000 + 
					"s more, due to sending too many invalid session cookies.");
			return;
		}
		
		LoginSession ls;
		try
		{
			ls = sessionMan.getSession(sessionId);
		} catch (WrongArgumentException e)
		{
			dosGauard.unsuccessfulAttempt(clientIp);
			gotoAuthn(httpRequest, httpResponse);
			return;
		}
		dosGauard.successfulAttempt(clientIp);
		if (session == null)
			session = httpRequest.getSession(true);
		session.setAttribute(WebSession.USER_SESSION_KEY, ls);
		AuthenticationProcessor.setupSessionCookie(sessionCookie, ls.getId(), httpResponse);
		sessionBinder.bindHttpSession(session, ls);
		
		gotoResource(httpRequest, response, chain);
	}

	private void gotoAuthn(HttpServletRequest httpRequest, HttpServletResponse response) throws IOException
	{
		if (log.isTraceEnabled())
			log.trace("Request to protected address, redirecting to auth: " + 
					httpRequest.getRequestURI());
		HttpSession session = httpRequest.getSession();
		session.setAttribute(ORIGINAL_ADDRESS, httpRequest.getRequestURI());
		response.sendRedirect(authnServletPath);
	}

	private void gotoResource(HttpServletRequest httpRequest, ServletResponse response, FilterChain chain) 
			throws IOException, ServletException
	{
		if (log.isTraceEnabled())
			log.trace("Request to protected address, user is authenticated: " + 
					httpRequest.getRequestURI());
		chain.doFilter(httpRequest, response);
	}
	
	private String getUnitySessionIdFromCookie(HttpServletRequest request)
	{
		Cookie[] cookies = request.getCookies();
		for (Cookie cookie: cookies)
			if (sessionCookie.equals(cookie.getName()))
				return cookie.getValue();
		return null;
	}
	
	
	public static boolean hasPathPrefix(String pathInfo , String prefix) {

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
