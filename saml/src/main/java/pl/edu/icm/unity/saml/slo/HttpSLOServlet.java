/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import pl.edu.icm.unity.saml.SamlHttpServlet;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;

/**
 * Implements HTTP POST and HTTP Redirect bindings entry point to the SLO functionality.
 * @author K. Benedyczak
 */
public class HttpSLOServlet extends SamlHttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, HttpSLOServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		process(true, req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		process(false, req, resp);
	}
	
	private void process(boolean isGet, HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		String samlResponse = req.getParameter("SAMLResponse");
		String samlRequest = req.getParameter("SAMLRequest");
		if (samlResponse == null && samlRequest == null)
		{
			log.warn("Got a request to the SAML Single Logout endpoint, " +
					"but neither request nor response is present");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Neither request nor response is present");
			return;
		}
		if (samlResponse != null && samlRequest != null)
		{
			log.warn("Got a request to the SAML Single Logout endpoint, " +
					"with both request and response present");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Both request and response are present");
			return;
		}
		
		if (samlResponse == null)
			processRequest(samlRequest, isGet, resp);
		else
			processResponse(samlResponse, isGet, resp);
	}
	
	private void processRequest(String samlRequest, boolean isGet, HttpServletResponse resp) throws IOException
	{
		String decoded = isGet ? extractRequestFromRedirectBinding(samlRequest) : 
			extractRequestFromPostBinding(samlRequest);
		
		try
		{
			LogoutRequestDocument reqDoc = LogoutRequestDocument.Factory.parse(decoded);
		} catch (XmlException e)
		{
			log.warn("Got a request to the SAML Single Logout endpoint, " +
					"with invalid request (XML is broken)", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid SLO request (XML is malformed)");
			return;
		}
		//TODO
	}

	private void processResponse(String samlResponse, boolean isGet, HttpServletResponse resp) throws IOException
	{
		String decoded = isGet ? extractResponseFromRedirectBinding(samlResponse) : 
			extractResponseFromPostBinding(samlResponse);
		try
		{
			LogoutResponseDocument respDoc = LogoutResponseDocument.Factory.parse(decoded);
		} catch (XmlException e)
		{
			log.warn("Got an invalid SAML Single Logout response (XML is broken)", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid SLO response (XML is malformed)");
		}
		//TODO
	}
}
