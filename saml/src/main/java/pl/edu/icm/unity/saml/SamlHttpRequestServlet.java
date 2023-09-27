/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;

import java.io.IOException;

/**
 * Generic SAML servlet. Provides support for low level parsing of both HTTP Redirect and HTTP POST bindings
 * supporting RelayState. Good foundation for extensions.
 * 
 * @author K. Benedyczak
 */
public abstract class SamlHttpRequestServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlHttpRequestServlet.class);
	
	private boolean requireRelayState = true;
	
	protected SamlHttpRequestServlet(boolean requireRelayState)
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
		String samlRequest = req.getParameter("SAMLRequest");
		if (samlRequest == null)
		{
			log.warn("Got a request to the SAML request consumer endpoint, " +
					"but no 'SAMLRequest' is present in HTTP message parameters.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'SAMLRequest' parameter");
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
		
		String decoded = isGet ? extractRequestFromRedirectBinding(samlRequest) : 
				extractRequestFromPostBinding(samlRequest);
		postProcessRequest(isGet, req, resp, decoded, relayState);
	}
	
	/**
	 * Needs to be implemented to perform a final processing. Arguments provide information on the binding,
	 * gives an extracted saml request and relay state which are guaranteed to be non-null.
	 * The SAML request is already decoded, i.e. it is raw XML. 
	 */
	protected abstract void postProcessRequest(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlRequest, String relayState) throws IOException;
	
	protected String extractRequestFromPostBinding(String samlResponseEncoded)
	{
		return SamlServletExtractionUtils.extractFromPostBinding(samlResponseEncoded, "request");
	}
	
	protected String extractRequestFromRedirectBinding(String samlResponseEncoded) throws IOException
	{
		return SamlServletExtractionUtils.extractFromRedirectBinding(samlResponseEncoded, "request");
	}
}
