/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.handler.ErrorHandler;

/**
 * Custom error handler - hiding server's details
 * @author K. Benedyczak
 */
public class UnityErrorHandler extends ErrorHandler
{
	@Override
	protected void writeErrorPageHead(HttpServletRequest request, Writer writer, int code,
			String message) throws IOException
	{
		writer.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\n");
		writer.write("<title> HTTP Error: ");
		writer.write(Integer.toString(code));
		writer.write(' ');
		write(writer, message);
		writer.write("</title>\n");
	}

	@Override
	protected void writeErrorPageBody(HttpServletRequest request, Writer writer, int code,
			String message, boolean showStacks) throws IOException
	{
		String uri = request.getRequestURI();
		writeErrorPageMessage(request, writer, code, message, uri);
		if (showStacks)
			writeErrorPageStacks(request, writer);
	}

	@Override
	protected void writeErrorPageMessage(HttpServletRequest request, Writer writer, int code,
			String message, String uri) throws IOException
	{
		writer.write("<h1 style=\"color: red;\">HTTP Error: ");
		writer.write(Integer.toString(code));
		writer.write("</h1>");
		writer.write("Error reason:\n<pre>    ");
		write(writer, message);
		writer.write("</pre>");
	}
}
