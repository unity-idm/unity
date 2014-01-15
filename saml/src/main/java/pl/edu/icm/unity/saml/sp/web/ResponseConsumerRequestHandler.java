/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import pl.edu.icm.unity.saml.sp.HttpRedirectBindingSupport;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.server.utils.Log;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

/**
 * Custom Vaadin {@link RequestHandler} which awaits SAML authn response from IdP, which should be 
 * attached to the HTTP request.
 * <p>
 * If the response is found it is confronted with the expected data from the SAML authentication context and 
 * if is OK it is recorded in the context so the UI can catch up and further process the response.
 * 
 * @author K. Benedyczak
 */
public class ResponseConsumerRequestHandler implements RequestHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ResponseConsumerRequestHandler.class);
	public static final String PATH = "/spSAMLResponseConsumer";
	
	@Override
	public boolean handleRequest(VaadinSession vaadinSession, VaadinRequest request,
			VaadinResponse response) throws IOException
	{
		if (!PATH.equals(request.getPathInfo()))
			return false;
		
		WrappedSession session = vaadinSession.getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context == null)
		{
			log.warn("Got a request to the ..." + PATH + " path, " +
					"but no SAML authn context is present in the session.");
			return false;
		}
		String method = request.getMethod();
		String samlResponse = request.getParameter("SAMLResponse");
		if (samlResponse == null)
		{
			log.warn("Got a request to the ..." + PATH + " path, " +
					"but no 'SAMLResponse' is present in HTTP message parameters.");
			return false;
		}
		if ("POST".equals(method))
		{
			handlePostBinding(samlResponse, context);
		}
		else if ("GET".equals(method))
		{
			handleRedirectBinding(samlResponse, context);
		}
		return false;
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



