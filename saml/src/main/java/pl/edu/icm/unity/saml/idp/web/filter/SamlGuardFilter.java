/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.BufferedReader;
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

import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Filter which is invoked prior to authentication. 
 * <p>
 * If a request comes to any other address then the SAML consumer servlet path, then the filter checks if a SAML context 
 * is available in the session. If not - the request is banned and an error page displayed.
 * 
 * @author K. Benedyczak
 */
public class SamlGuardFilter implements Filter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlGuardFilter.class);
	
	protected ErrorHandler errorHandler;

	public SamlGuardFilter(ErrorHandler errorHandler)
	{
		super();
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
		HttpSession session = request.getSession();
		SAMLAuthnContext context = (SAMLAuthnContext) session.getAttribute(
				SamlParseServlet.SESSION_SAML_CONTEXT); 

		if (context == null)
		{
			if (log.isDebugEnabled())
			{
				log.debug("Request to SAML post-processing address, without SAML context: " 
						+ request.getRequestURI() + "?" + request.getQueryString());
				if (log.isTraceEnabled())
					dumpRequest(request);
			}
			errorHandler.showErrorPage(new SAMLProcessingException("No SAML context"), 
					(HttpServletResponse) response);
			return;
		} else
		{
			if (log.isTraceEnabled())
				log.trace("Request to SAML post-processing address, with SAML context: " 
						+ request.getRequestURI());
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
	
	private void dumpRequest(HttpServletRequest request) throws IOException
	{
		StringBuilder sb = new StringBuilder(); 
		BufferedReader br = new BufferedReader(request.getReader());
		String line;
		while ((line = br.readLine()) != null)
			sb.append(line);
		log.trace("Blocked request info:\n" + request.getMethod());
		log.trace("Blocked request params:\n" + request.getParameterMap());
		log.trace("Blocked request contents:\n" + sb);
	}
}
