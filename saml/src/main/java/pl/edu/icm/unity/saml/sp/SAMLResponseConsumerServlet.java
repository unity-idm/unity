/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Custom servlet which awaits SAML authn response from IdP, which should be 
 * attached to the HTTP request.
 * <p>
 * If the response is found it is confronted with the expected data from the SAML authentication context and 
 * if is OK it is recorded in the context so the UI can catch up and further process the response.
 * 
 * @author K. Benedyczak
 */
public class SAMLResponseConsumerServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLResponseConsumerServlet.class);
	public static final String PATH = "/spSAMLResponseConsumer";
	
	private SamlContextManagement contextManagement;
	
	public SAMLResponseConsumerServlet(SamlContextManagement contextManagement)
	{
		this.contextManagement = contextManagement;
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
		if (relayState == null)
		{
			log.warn("Got a request to the SAML response consumer endpoint, " +
					"but no 'RelayState' is present in HTTP message parameters.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'RelayState' parameter");
			return;
		}
		
		RemoteAuthnContext context;
		try
		{
			context = contextManagement.getAuthnContext(relayState);
		} catch (WrongArgumentException e)
		{
			log.warn("Got a request to the SAML response consumer endpoint, " +
					"with invalid relay state.");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Wrong 'RelayState' value");
			return;
		}
		
		if (isGet)
			handleRedirectBinding(samlResponse, context);
		else
			handlePostBinding(samlResponse, context);
		
		resp.sendRedirect(context.getReturnUrl());
	}
	
	private void handlePostBinding(String samlResponseEncoded, RemoteAuthnContext context)
	{
		String samlResponse = new String(Base64.decode(samlResponseEncoded));
		if (log.isTraceEnabled())
			log.trace("Got SAML response using the HTTP POST binding:\n" + samlResponse);
		else
			log.debug("Got SAML response using the HTTP POST binding");
		context.setResponse(samlResponse, Binding.HTTP_POST);
	}
	
	private void handleRedirectBinding(String samlResponseEncoded, RemoteAuthnContext context)
	{
		String samlResponseDecoded;
		try
		{
			samlResponseDecoded = HttpRedirectBindingSupport.inflateSAMLRequest(samlResponseEncoded);
		} catch (IOException e)
		{
			log.warn("Got an improperly encoded SAML response (using HTTP Redirect binding), " +
					"ignoring it.", e);
			return;
		}
		if (log.isTraceEnabled())
			log.trace("Got SAML response using the HTTP Redirect binding:\n" + samlResponseDecoded);
		else
			log.debug("Got SAML response using the HTTP Redirect binding");
		context.setResponse(samlResponseDecoded, Binding.HTTP_REDIRECT);
	}
}



