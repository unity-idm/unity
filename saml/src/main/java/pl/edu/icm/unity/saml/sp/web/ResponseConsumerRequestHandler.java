/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;

import pl.edu.icm.unity.saml.sp.SAMLValidator;

import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;

/**
 * Custom Vaadin {@link RequestHandler} which awaits SAML authn response from IdP, which should be 
 * attached to the HTTP request.
 * <p>
 * If the response is found it is confronted with the expected data from the SAML authentication context and 
 * if is OK it is passed for validation to the {@link SAMLValidator}. The validation result is recorded in the context
 * so the UI can catch up.
 * 
 * @author K. Benedyczak
 */
public class ResponseConsumerRequestHandler implements RequestHandler
{
	public static final String PATH = "/spSAMLResponseConsumer";
	
	@Override
	public boolean handleRequest(VaadinSession session, VaadinRequest request,
			VaadinResponse response) throws IOException
	{
		// TODO Auto-generated method stub
		return false;
	}

}
