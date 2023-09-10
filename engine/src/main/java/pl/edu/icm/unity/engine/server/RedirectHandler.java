/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Request;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Simple handler redirecting request without path to a pre-configured path.
 * 
 * @author P.Piernik
 */
public class RedirectHandler extends ServletContextHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, RedirectHandler.class);
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