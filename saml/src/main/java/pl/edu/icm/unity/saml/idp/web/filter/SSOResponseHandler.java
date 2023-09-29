/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.security.dsig.DSigException;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import pl.edu.icm.unity.saml.idp.web.SamlSessionService;
import pl.edu.icm.unity.saml.slo.SamlMessageHandler;
import pl.edu.icm.unity.saml.slo.SamlRoutableMessage;
import pl.edu.icm.unity.saml.slo.SamlRoutableUnsignedMessage;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Helper to send responses in SSO authn case, when working in non-Vaadin
 * environment (plain servlets).
 * 
 * @author K. Benedyczak
 */
public class SSOResponseHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SSOResponseHandler.class);
	private final SamlMessageHandler messageHandler;
	private final SamlIdpStatisticReporter reporter;

	public SSOResponseHandler(FreemarkerAppHandler freemarker, SamlIdpStatisticReporterFactory reporterFactory,
			Endpoint endpoint)
	{
		messageHandler = new SamlMessageHandler(freemarker);
		this.reporter = reporterFactory.getForEndpoint(endpoint);
	}

	public void sendResponse(SAMLAuthnContext samlCtx, SamlRoutableMessage response, Binding binding,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws IOException, EopException, DSigException
	{
		try
		{
			messageHandler.sendResponse(binding, response, httpResponse, "SSO Authentication response");
			reporter.reportStatus(samlCtx, Status.SUCCESSFUL);
		} finally
		{
			cleanContext(httpRequest, false);
		}
	}

	public void handleException(AuthnResponseProcessor samlProcessor, Exception e, Binding binding, String serviceUrl,
			SAMLAuthnContext samlCtx, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			boolean invalidate) throws EopException, IOException
	{
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		SamlRoutableUnsignedMessage response = new SamlRoutableUnsignedMessage(respDoc, SAMLMessageType.SAMLResponse,
				samlCtx.getRelayState(), serviceUrl);
		log.warn("Sending SAML error to {} in effect of exception handling", serviceUrl, e);
		try
		{
			messageHandler.sendResponse(binding, response, httpResponse, "SSO Authentication error response");
			reporter.reportStatus(samlCtx, Status.FAILED);
		} catch (DSigException e1)
		{
			throw new IllegalStateException("DSIG on unsigned request shouldn't happen", e);
		} finally
		{
			cleanContext(httpRequest, invalidate);
		}
	}

	private void cleanContext(HttpServletRequest httpRequest, boolean invalidate)
	{
		SamlSessionService.cleanContext(new LoginInProgressService.HttpContextSession(httpRequest));
		if (invalidate)
			httpRequest.getSession().invalidate();
	}
}
