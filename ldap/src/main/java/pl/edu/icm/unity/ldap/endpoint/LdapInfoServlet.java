/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.log4j.Logger;
import pl.edu.icm.unity.server.utils.Log;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 */
public class LdapInfoServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapInfoServlet.class);

	public LdapInfoServlet()
	{
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		log.trace("Received GET request to the Ldap info endpoint");
		PrintWriter writer = resp.getWriter();
		writer.print("Info");
		writer.flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		log.trace("Received POST request to the Ldap info endpoint");
	}
}
