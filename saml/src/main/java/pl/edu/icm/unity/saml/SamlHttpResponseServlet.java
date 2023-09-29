/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Generic SAML response servlet. Provides support for low level parsing of both HTTP Redirect and HTTP POST bindings
 * supporting RelayState. Should be extended providing actual processing logic.
 */
public abstract class SamlHttpResponseServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlHttpResponseServlet.class);
	
	private boolean requireRelayState;
	
	protected SamlHttpResponseServlet(boolean requireRelayState)
	{
		this.requireRelayState = requireRelayState;
	}

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
		if (samlResponse == null)
		{
			log.warn("Got a request to the SAML response consumer endpoint, " +
					"but no 'SAMLResponse' is present in HTTP message parameters.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'SAMLResponse' parameter");
			return;
		}
		
		String relayState = req.getParameter("RelayState");
		if (requireRelayState && relayState == null)
		{
			log.warn("Got a request to the SAML response consumer endpoint, " +
					"but no 'RelayState' is present in HTTP message parameters.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'RelayState' parameter");
			return;
		}
		
		String decoded = isGet ? extractResponseFromRedirectBinding(samlResponse) : 
				extractResponseFromPostBinding(samlResponse);
		postProcessResponse(isGet, req, resp, decoded, relayState);
	}
	
	/**
	 * Needs to be implemented to perform a final processing. Arguments provide information on the binding,
	 * gives an extracted saml response and relay state which are guaranteed to be non-null.
	 * The SAML response is already decoded, i.e. it is raw XML. 
	 */
	protected abstract void postProcessResponse(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlResponse, String relayState) throws IOException;

	private String extractResponseFromPostBinding(String samlResponseEncoded)
	{
		return SamlServletExtractionUtils.extractFromPostBinding(samlResponseEncoded, "response");
	}
	
	private String extractResponseFromRedirectBinding(String samlResponseEncoded) throws IOException
	{
		return SamlServletExtractionUtils.extractFromRedirectBinding(samlResponseEncoded, "response");
	}
}
