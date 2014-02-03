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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.WebSession;

/**
 * Servlet filter redirecting unauthenticated requests to the protected addresses,
 * to the authentication servlet.
 * @author K. Benedyczak
 */
public class AuthenticationFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticationFilter.class);
	private String protectedPath;
	private String authnServletPath;
	public static final String ORIGINAL_ADDRESS = AuthenticationFilter.class.getName()+".origURIkey";

	public AuthenticationFilter(String protectedPath, String authnServletPath)
	{
		this.protectedPath = protectedPath;
		this.authnServletPath = authnServletPath;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String servletPath = httpRequest.getServletPath();
		if (!hasPathPrefix(servletPath, protectedPath))
		{
			if (log.isTraceEnabled())
				log.trace("Request to not protected address: " + httpRequest.getRequestURI());
			chain.doFilter(httpRequest, response);
			return;
		}

		HttpSession session = httpRequest.getSession(false);
		if (session == null || session.getAttribute(WebSession.USER_SESSION_KEY) == null)
		{
			if (log.isTraceEnabled())
				log.trace("Request to protected address, redirecting to auth: " + 
						httpRequest.getRequestURI());
			session = httpRequest.getSession();
			session.setAttribute(ORIGINAL_ADDRESS, httpRequest.getRequestURI());
			((HttpServletResponse)response).sendRedirect(authnServletPath);
		} else
		{
			if (log.isTraceEnabled())
				log.trace("Request to protected address, user is authenticated: " + 
						httpRequest.getRequestURI());
			chain.doFilter(httpRequest, response);
		}
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
