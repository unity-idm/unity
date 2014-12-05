/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.saml.SamlHttpServlet;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
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
public class SAMLResponseConsumerServlet extends SamlHttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLResponseConsumerServlet.class);
	public static final String PATH = "/spSAMLResponseConsumer";
	
	private SamlContextManagement contextManagement;
	
	public SAMLResponseConsumerServlet(SamlContextManagement contextManagement)
	{
		super(false, true, true);
		this.contextManagement = contextManagement;
	}

	@Override
	protected void postProcessResponse(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlResponse, String relayState) throws IOException
	{
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
		
		Binding binding = isGet ? Binding.HTTP_REDIRECT : Binding.HTTP_POST;
		context.setResponse(samlResponse, binding);
		resp.sendRedirect(context.getReturnUrl());
	}
}

