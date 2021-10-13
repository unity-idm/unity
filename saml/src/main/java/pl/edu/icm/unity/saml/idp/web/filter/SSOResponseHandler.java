/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.security.dsig.DSigException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticEvent;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.SamlSessionService;
import pl.edu.icm.unity.saml.slo.SamlMessageHandler;
import pl.edu.icm.unity.saml.slo.SamlRoutableMessage;
import pl.edu.icm.unity.saml.slo.SamlRoutableUnsignedMessage;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.webui.LoginInProgressService.HttpContextSession;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Helper to send responses in SSO authn case, when working in non-Vaadin environment (plain servlets).
 * @author K. Benedyczak
 */
public class SSOResponseHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SSOResponseHandler.class);
	private final SamlMessageHandler messageHandler;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Endpoint endpoint;
	private final MessageSource msg;
	
	public SSOResponseHandler(FreemarkerAppHandler freemarker, ApplicationEventPublisher applicationEventPublisher, MessageSource msg,
			Endpoint endpoint)
	{
		messageHandler = new SamlMessageHandler(freemarker);
		this.applicationEventPublisher = applicationEventPublisher;
		this.msg = msg;
		this.endpoint = endpoint;
	}

	public void sendResponse(SAMLAuthnContext samlCtx, SamlRoutableMessage response, Binding binding,  
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
					throws IOException, EopException, DSigException
	{
		try
		{
			messageHandler.sendResponse(binding, response, httpResponse, "SSO Authentication response");
			reportStatus(samlCtx, Status.SUCCESSFUL);
		} finally
		{
			cleanContext(httpRequest, false);
		}
	}
	
	public void handleException(AuthnResponseProcessor samlProcessor,
			Exception e, Binding binding, String serviceUrl, 
			SAMLAuthnContext samlCtx, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			boolean invalidate) 
					throws EopException, IOException
	{
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		SamlRoutableUnsignedMessage response = new SamlRoutableUnsignedMessage(respDoc,  
				SAMLMessageType.SAMLResponse, samlCtx.getRelayState(), serviceUrl);
		log.warn("Sending SAML error to {} in effect of exception handling", serviceUrl, e);
		try
		{
			messageHandler.sendResponse(binding, response, httpResponse, 
					"SSO Authentication error response");
			reportStatus(samlCtx, Status.FAILED);
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
		SamlSessionService.cleanContext(new HttpContextSession(httpRequest));
		if (invalidate)
			httpRequest.getSession().invalidate();
	}
	
	protected void reportStatus(SAMLAuthnContext samlCtx, Status status)
	{
		applicationEventPublisher.publishEvent(new IdpStatisticEvent(endpoint.getName(),
				endpoint.getConfiguration().getDisplayedName() != null
						? endpoint.getConfiguration().getDisplayedName().getValue(msg)
						: null,
				samlCtx.getRequest().getIssuer().getStringValue(),
				samlCtx.getSamlConfiguration().getDisplayedNameForRequester(samlCtx.getRequest().getIssuer()),
				status));
	}
		
}
