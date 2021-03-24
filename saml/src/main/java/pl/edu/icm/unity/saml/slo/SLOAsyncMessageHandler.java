/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.LogoutResponse;
import eu.unicore.security.dsig.DSigException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;

/**
 * Prepares and return LogoutResponses for the asynchronous bindings. 
 * Also support for showing an error page directly is provided with freemarker help.
 * 
 * @author K. Benedyczak
 */
class SLOAsyncMessageHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOAsyncMessageHandler.class);
	private final SamlMessageHandler messageHandler;
	
	SLOAsyncMessageHandler(FreemarkerAppHandler freemarker)
	{
		this.messageHandler = new SamlMessageHandler(freemarker);
	}

	/**
	 * Shows a page with error.
	 */
	void showError(SAMLProcessingException error, HttpServletResponse response) 
			throws IOException, EopException
	{
		log.warn("SAML error is going to be shown to the user redirected to Unity SLO endpoint", error);
		messageHandler.showError(error, response);
	}	
	
	/**
	 * Return a logout error response to the requester via async binding. The error is produced from the
	 * exception provided as an argument. At the end the {@link EopException} 
	 * is always thrown to break any further processing.
	 */
	void sendErrorResponse(Binding binding, SAMLServerException error, String serviceUrl, 
			SAMLExternalLogoutContext context, HttpServletResponse response) 
					throws IOException, EopException
	{
		sendErrorResponse(binding, error, serviceUrl, context.getLocalSessionAuthorityId(), 
				context.getRequestersRelayState(), context.getRequest().getID(), response);
	}

	void sendErrorResponse(Binding binding, SAMLServerException error, String serviceUrl, 
			String localIssuer, String relayState, String requestId, 
			HttpServletResponse response) throws IOException, EopException
	{
		log.warn("SAML error is going to be returned to the SAML requester from SLO endpoint", error);
		LogoutResponse errorResp = new LogoutResponse(new NameID(localIssuer, null).getXBean(), 
				requestId, error);
		SamlRoutableSignableMessage<LogoutResponseDocument> message = new SamlRoutableSignableMessage<>(
				errorResp, null, SAMLMessageType.SAMLResponse, relayState, serviceUrl);
		try
		{
			messageHandler.sendResponse(binding, message, response, "Logout Error");
		} catch (DSigException e)
		{
			throw new IllegalStateException("Can't send SAML error due to signature problem. Shouldn't happen.", e);
		}
	}

	void sendRequest(Binding binding, SamlRoutableSignableMessage<LogoutRequestDocument> request, 
			HttpServletResponse response) throws IOException, EopException, DSigException
	{
		messageHandler.sendRequest(binding, request, response, "Logout");
	}

	void sendResponse(Binding binding, SamlRoutableSignableMessage<LogoutResponseDocument> samlResponse, 
			HttpServletResponse httpResponse) throws IOException, EopException, DSigException
	{
		messageHandler.sendResponse(binding, samlResponse, httpResponse, "Logout");
	}
}
