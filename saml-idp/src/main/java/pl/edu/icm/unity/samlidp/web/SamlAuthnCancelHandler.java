/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.web;

import java.util.Calendar;

import pl.edu.icm.unity.exceptions.AuthenticationException;
import pl.edu.icm.unity.samlidp.FreemarkerHandler;
import pl.edu.icm.unity.samlidp.saml.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.webui.authn.CancelHandler;

/**
 * Implements handling of cancelation of authentication in the context of SAML processing.
 *  
 * @author K. Benedyczak
 */
public class SamlAuthnCancelHandler implements CancelHandler
{
	private FreemarkerHandler freemarkerHandler;
	private String address;
	
	public SamlAuthnCancelHandler(FreemarkerHandler freemarkerHandler, String address)
	{
		this.freemarkerHandler = freemarkerHandler;
		this.address = address;
	}

	@Override
	public void onCancel()
	{
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(SamlResponseHandler.getContext(), 
				Calendar.getInstance());
		SamlResponseHandler responseHandler = new SamlResponseHandler(freemarkerHandler, samlProcessor, address);
		AuthenticationException ea = new AuthenticationException("Authentication was declined");
		try
		{
			responseHandler.handleException(ea, false);
		} catch (EopException e)
		{
			//OK - nothing to do.
			return;
		}
	}
}
