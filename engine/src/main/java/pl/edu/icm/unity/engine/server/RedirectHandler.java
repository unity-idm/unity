/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

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
	public boolean handle(Request request, Response response, Callback callback) throws Exception
	{
		String target = request.getHttpURI()
				.getPath();
		if (target != null && (target.equals("/") || target.isEmpty()))
		{
			if (log.isTraceEnabled())
				log.trace("Redirect from " + request.getHttpURI() + " -> " + redirectPath);
			Response.sendRedirect(request, response, callback, redirectPath);
			return true;
		}
		return false;
	}
}