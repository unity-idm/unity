/*
 * Copyright (c) 2011 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21-01-2011
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */
package pl.edu.icm.unity.server;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleServlet extends HttpServlet
{
	public static final String OK_GET = "OK-GET";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException
	{
		write(OK_GET, resp);
	}
	
	private void write(String what, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("text/plain");
		resp.setContentLength(what.length());
		PrintStream out = new PrintStream(resp.getOutputStream());
		out.print(what);
		out.flush();
		resp.flushBuffer();
	}
}
