/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.Calendar;
import java.util.TimeZone;

import org.springframework.context.ApplicationEventPublisher;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.idpcommon.EopException;

/**
 * Implements handling of cancellation of authentication in the context of SAML processing.
 *  
 * @author K. Benedyczak
 */
public class SamlAuthnCancelHandler implements CancelHandler
{
	private final FreemarkerAppHandler freemarkerHandler;
	private final AttributeTypeSupport aTypeSupport;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final MessageSource msg;
	private final Endpoint endpoint;
	
	public SamlAuthnCancelHandler(FreemarkerAppHandler freemarkerHandler, AttributeTypeSupport aTypeSupport,
			ApplicationEventPublisher applicationEventPublisher, MessageSource msg, Endpoint endpoint)
	{
		this.freemarkerHandler = freemarkerHandler;
		this.aTypeSupport = aTypeSupport;
		this.applicationEventPublisher = applicationEventPublisher;
		this.msg = msg;
		this.endpoint = endpoint;			
	}

	@Override
	public void onCancel()
	{
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, 
				SamlSessionService.getVaadinContext(), 
				Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		SamlResponseHandler responseHandler = new SamlResponseHandler(freemarkerHandler, samlProcessor, applicationEventPublisher, msg, endpoint);
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
