/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.server;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletContextHandler;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration.RedirectMode;

/**
 * Simply handler for redirect or forward request without path to configured
 * path
 * 
 * @author P.Piernik
 *
 */
public class RedirectHandler extends ServletContextHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, UnityApplication.class);
	private RedirectMode redirectMode;
	private String redirectPath;

	public RedirectHandler(RedirectMode mode, String toPath)
	{
		setContextPath("/*");
		redirectPath = toPath;
		redirectMode = mode;
	}

	@Override
	public void doHandle(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException
	{
		switch (redirectMode)
		{
		case FORWARD:
			
			ServletContext rootContext = getServletContext().getContext(redirectPath);
			RequestDispatcher reqDisp = rootContext
					.getRequestDispatcher(request.getRequestURI());

			log.debug("Forward from " + request.getRequestURI() + " to "
					+ rootContext.getContextPath() + request.getRequestURI());

			reqDisp.forward(baseRequest, response);
			return;

		case REDIRECT:
			
			log.debug("Redirect from " + request.getRequestURI() + " to " + redirectPath);
			response.sendRedirect(redirectPath);
			return;
		}

	}

}
