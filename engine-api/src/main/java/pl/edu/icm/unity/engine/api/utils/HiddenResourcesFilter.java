/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Servlet filter blocking access to all configured resources. The purpose is to hide servlets 
 * which should be accessible only by internal forwards. To achieve this, this filter should be installed 
 * to protect them, with the request scope.
 * 
 * @author K. Benedyczak
 */
public class HiddenResourcesFilter implements Filter
{
	private static final List<String> PUSH_AND_HEARTBEAT_PARAMETERS = List.of("push", "heartbeat");
	private List<String> protectedServletPaths;
	
	public HiddenResourcesFilter(List<String> protectedServletPaths)
	{
		this.protectedServletPaths = protectedServletPaths;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		String servletPath = httpRequest.getServletPath();
		if (hasPathPrefix(servletPath, protectedServletPaths))
		{
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, 
					"The requested address is not available.");
			return;
		}
		chain.doFilter(httpRequest, httpResponse);
	}

	public static boolean hasPathPrefix(String pathInfo , List<String> prefixes)
	{
		for (String prefix : prefixes) 
		{
			if (hasPathPrefix(pathInfo, prefix))
			{
				return true;
			}
		}
		return false;
	}
	
	public static boolean hasPathPrefix(String pathInfo , String prefix) 
	{
		if (pathInfo == null || pathInfo.equals("")) 
		{
			return false;
		}

		if (!prefix.startsWith("/")) 
		{
			prefix = '/' + prefix;
		}

		if (pathInfo.startsWith(prefix)) 
		{
			return true;
		}

		return false;
	}

	public static boolean isPushOrHeartbeatV23Request(HttpServletRequest request)
	{
		return PUSH_AND_HEARTBEAT_PARAMETERS.contains(ofNullable(request.getParameter("v-r")).orElse(""));
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}
	
	public void addProtectedPath(String path)
	{
		protectedServletPaths.add(path);
	}
}
