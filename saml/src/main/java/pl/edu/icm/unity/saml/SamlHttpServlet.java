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

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;

/**
 * Generic SAML servlet. It provides support for low level parsing of both HTTP Redirect and HTTP POST bindings
 * supporting RelayState and both request and response parsing. Good foundation for extensions.
 * 
 * @author K. Benedyczak
 */
public abstract class SamlHttpServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlHttpServlet.class);
	
	private boolean requireSamlRequest;
	private boolean requireSamlResponse;
	
	protected SamlHttpServlet(boolean requireSamlRequest, boolean requireSamlResponse)
	{
		this.requireSamlRequest = requireSamlRequest;
		this.requireSamlResponse = requireSamlResponse;
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
	
	protected void process(boolean isGet, HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		String samlResponse = req.getParameter("SAMLResponse");
		if (samlResponse == null && requireSamlResponse)
		{
			log.warn("Got a request to the SAML response consumer endpoint, " +
					"but no 'SAMLResponse' is present in HTTP message parameters.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'SAMLResponse' parameter");
			return;
		}
		String samlRequest = req.getParameter("SAMLRequest");
		if (samlRequest == null && requireSamlRequest)
		{
			log.warn("Got a request to the SAML request consumer endpoint, " +
					"but no 'SAMLRequest' is present in HTTP message parameters.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'SAMLRequest' parameter");
			return;
		}
		
		if (samlRequest != null && samlResponse != null)
		{
			log.warn("Got a request to the SAML endpoint with both SAML request and response. What?");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Decide what you want, please");
			return;
		}
		
		String relayState = req.getParameter("RelayState");
		if (relayState == null)
		{
			log.warn("Got a request to the SAML response consumer endpoint, " +
					"but no 'RelayState' is present in HTTP message parameters.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'RelayState' parameter");
			return;
		}
		
		if (samlResponse != null)
		{
			String decoded = isGet ? extractResponseFromRedirectBinding(samlResponse) : 
				extractResponseFromPostBinding(samlResponse);
			postProcessResponse(isGet, req, resp, decoded, relayState);
		} else if (samlRequest != null)
		{
			String decoded = isGet ? extractRequestFromRedirectBinding(samlRequest) : 
				extractRequestFromPostBinding(samlRequest);
			postProcessRequest(isGet, req, resp, decoded, relayState);
		}
	}
	
	/**
	 * Needs to be implemented to perform a final processing. Arguments provide information on the binding,
	 * gives an extracted saml response and relay state which are guaranteed to be non-null.
	 * The SAML response is already decoded, i.e. it is raw XML. 
	 * @param isGet
	 * @param req
	 * @param resp
	 * @param samlResponse
	 * @param relayState
	 * @throws IOException
	 */
	protected void postProcessResponse(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlResponse, String relayState) throws IOException
	{	
	}

	/**
	 * Needs to be implemented to perform a final processing. Arguments provide information on the binding,
	 * gives an extracted saml request and relay state which are guaranteed to be non-null.
	 * The SAML request is already decoded, i.e. it is raw XML. 
	 * @param isGet
	 * @param req
	 * @param resp
	 * @param samlResponse
	 * @param relayState
	 * @throws IOException
	 */
	protected void postProcessRequest(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlRequest, String relayState) throws IOException
	{	
	}
	
	protected String extractResponseFromPostBinding(String samlResponseEncoded)
	{
		return extractFromPostBinding(samlResponseEncoded, "response");
	}
	
	protected String extractRequestFromPostBinding(String samlResponseEncoded)
	{
		return extractFromPostBinding(samlResponseEncoded, "request");
	}
	
	protected String extractFromPostBinding(String samlResponseEncoded, String what)
	{
		String samlResponse = new String(Base64.decode(samlResponseEncoded));
		if (log.isTraceEnabled())
			log.trace("Got SAML " + what + " using the HTTP POST binding:\n" + samlResponse);
		else
			log.debug("Got SAML " + what + " using the HTTP POST binding");
		return samlResponse;
	}
	
	protected String extractResponseFromRedirectBinding(String samlResponseEncoded) throws IOException
	{
		return extractFromRedirectBinding(samlResponseEncoded, "response");
	}

	protected String extractRequestFromRedirectBinding(String samlResponseEncoded) throws IOException
	{
		return extractFromRedirectBinding(samlResponseEncoded, "request");
	}
	
	protected String extractFromRedirectBinding(String samlResponseEncoded, String what) throws IOException
	{
		String samlResponseDecoded = HttpRedirectBindingSupport.inflateSAMLRequest(samlResponseEncoded);
		if (log.isTraceEnabled())
			log.trace("Got SAML " + what + " using the HTTP Redirect binding:\n" + samlResponseDecoded);
		else
			log.debug("Got SAML " + what + " using the HTTP Redirect binding");
		return samlResponseDecoded;
	}
}
