/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import io.imunity.vaadin.endpoint.common.EopException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter which is invoked prior to authentication. 
 * <p>
 * The filter checks if OAuth context is available in the session. If not - the request is banned and an error page displayed.
 */
public class OAuthGuardFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthGuardFilter.class);
	
	protected final ErrorHandler errorHandler;

	public OAuthGuardFilter(ErrorHandler errorHandler)
	{
		this.errorHandler = errorHandler;
	}

	@Override
	public void doFilter(ServletRequest requestBare, ServletResponse responseBare, FilterChain chain)
			throws IOException, ServletException
	{
		try
		{
			doFilterInterruptible(requestBare, responseBare, chain);
		} catch (EopException e)
		{
			//OK, that's fine, response was already committed
		}
	}
	
	protected void doFilterInterruptible(ServletRequest requestBare, ServletResponse responseBare, FilterChain chain)
			throws IOException, ServletException, EopException
	{
		if (!(requestBare instanceof HttpServletRequest))
			throw new ServletException("This filter can be used only for HTTP servlets");
		HttpServletRequest request = (HttpServletRequest) requestBare;
		HttpServletResponse response = (HttpServletResponse) responseBare;
		
		Optional<OAuthAuthzContext> context = OAuthSessionService.getContext(request); 
		if (!context.isPresent())
		{
			if (log.isDebugEnabled())
				log.warn("Request to OAuth post-processing address, without OAuth context: " + request.getRequestURI());
			errorHandler.showErrorPage("No OAuth context", null, response);
			return;
		} else
		{
			if (log.isTraceEnabled())
				log.trace("Request to OAuth post-processing address, with OAuth context: " + request.getRequestURI());
			chain.doFilter(request, response);
			return;
		}
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void destroy()
	{
	}
}
