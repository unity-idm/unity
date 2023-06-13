/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.Calendar;
import java.util.TimeZone;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.idpcommon.EopException;

/**
 * Implements handling of cancellation of authentication in the context of SAML
 * processing.
 * 
 * @author K. Benedyczak
 */
public class SamlAuthnCancelHandler implements CancelHandler
{
	private final FreemarkerAppHandler freemarkerHandler;
	private final AttributeTypeSupport aTypeSupport;
	private final Endpoint endpoint;
	private final SamlIdpStatisticReporterFactory reporterFactory;
	private final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;

	public SamlAuthnCancelHandler(FreemarkerAppHandler freemarkerHandler, AttributeTypeSupport aTypeSupport,
			SamlIdpStatisticReporterFactory reporterFactory, LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement, Endpoint endpoint)
	{
		this.freemarkerHandler = freemarkerHandler;
		this.aTypeSupport = aTypeSupport;
		this.endpoint = endpoint;
		this.reporterFactory = reporterFactory;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
	}

	@Override
	public void onCancel()
	{
		AuthnResponseProcessor samlProcessor = new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement,
				SamlSessionService.getVaadinContext(), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
		SamlResponseHandler responseHandler = new SamlResponseHandler(freemarkerHandler, samlProcessor, reporterFactory,
				endpoint);
		AuthenticationException ea = new AuthenticationException("Authentication was declined");
		try
		{
			responseHandler.handleException(ea, false);
		} catch (EopException e)
		{
			// OK - nothing to do.
			return;
		}
	}
}
