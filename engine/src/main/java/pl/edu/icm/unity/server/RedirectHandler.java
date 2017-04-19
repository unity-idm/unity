/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletContextHandler;

import pl.edu.icm.unity.server.utils.Log;

/**
 * Simple handler redirecting request without path to a pre-configured path.
 * 
 * @author P.Piernik
 */
public class RedirectHandler extends ServletContextHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, RedirectHandler.class);
	private String redirectPath;

	public RedirectHandler(String toPath)
	{
		setContextPath("/*");
		redirectPath = toPath;
	}

	@Override
	public void doHandle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException
	{

		if (target != null && (target.equals("/") || target.isEmpty()))
		{
			if (log.isTraceEnabled())
				log.trace("Redirect from " + request.getRequestURI() + " -> " 
					+ redirectPath);
			response.sendRedirect(redirectPath);
		}

	}

}