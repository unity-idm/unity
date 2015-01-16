/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

/**
 * Simple routing (dispatching) servlet. Requires a default target servlet path and can have additional 
 * servlets registered. Should be installed as an entry point for externally visible path. Subsequently it
 * routes all requests to a current destination. The destination can be changed with a customized forward.
 * <p>
 * Internally stores state in a single session variable.
 * 
 * @author K. Benedyczak
 */
public class RoutingServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, RoutingServlet.class);
	public static final String CURRENT_DESTINATION = RoutingServlet.class.getName() + ".destination";
	private String defaultTarget;

	public RoutingServlet(String defaultTarget)
	{
		this.defaultTarget = defaultTarget;
	}

	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		HttpSession session = req.getSession(false);
		if (session != null)
		{
			String destination = (String) session.getAttribute(CURRENT_DESTINATION);
			if (destination != null)
			{
				log.info("Routing request to regular destination " + destination);
				req.getRequestDispatcher(constructPath(destination, req)).forward(req, resp);
				return;
			}
		}
		log.info("Routing request to DEFAULT destination " + defaultTarget);
		req.getRequestDispatcher(constructPath(defaultTarget, req)).forward(req, resp);
	}
	
	private String constructPath(String destination, HttpServletRequest req)
	{
		String pathInfo = req.getPathInfo();
		return pathInfo == null ? destination : destination + pathInfo;   
	}
	
	/**
	 * The routing will be reconfigured to forward all requests to the given destination.
	 * @param newDestination
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	public static void forwardTo(String newDestination, HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException
	{
		req.getSession().setAttribute(CURRENT_DESTINATION, newDestination);
		req.getRequestDispatcher(newDestination).forward(req, resp);
	}

	/**
	 * Cleans any previous routing settings. Useful when a new interaction is started, but when the old session 
	 * is still used.
	 * @param req
	 */
	public static void clean(HttpServletRequest req) 
	{
		HttpSession session = req.getSession(false);
		if (session != null)
			session.removeAttribute(CURRENT_DESTINATION);
	}
}
