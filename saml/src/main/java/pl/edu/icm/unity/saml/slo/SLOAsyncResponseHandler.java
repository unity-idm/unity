/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.LogoutResponse;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.web.ResponseHandlerBase;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;

/**
 * Prepares and return LogoutResponses for the asynchronous bindings. 
 * Also support for showing an error page directly is provided with freemarker help.
 * 
 * @author K. Benedyczak
 */
public class SLOAsyncResponseHandler extends ResponseHandlerBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOAsyncResponseHandler.class);
	
	public SLOAsyncResponseHandler(FreemarkerAppHandler freemarker)
	{
		super(freemarker);
	}

	/**
	 * Shows a page with error.
	 * @param error
	 * @param context
	 * @param response
	 * @throws SAMLProcessingException
	 * @throws IOException
	 * @throws EopException
	 */
	public void showError(SAMLProcessingException error, HttpServletResponse response) 
			throws IOException, EopException
	{
		log.debug("SAML error is going to be shown to the user redirected to Unity SLO endpoint", error);
		super.showError(error, response);
	}	
	
	/**
	 * Return a logout error response to the requester via async binding. The error is produced from the
	 * exception provided as an argument. At the end the {@link EopException} 
	 * is always thrown to break any further processing.
	 * @param error
	 * @param serviceUrl
	 * @param context
	 * @param response
	 * @throws IOException
	 * @throws EopException
	 */
	public void sendErrorResponse(Binding binding, SAMLServerException error, String serviceUrl, 
			SAMLExternalLogoutContext context, HttpServletResponse response) throws IOException, EopException
	{
		sendErrorResponse(binding, error, serviceUrl, context.getLocalSessionAuthorityId(), 
				context.getRequestersRelayState(), context.getRequest().getID(), response);
	}

	public void sendErrorResponse(Binding binding, SAMLServerException error, String serviceUrl, 
			String localIssuer, String relayState, String requestId, 
			HttpServletResponse response) throws IOException, EopException
	{
		log.debug("SAML error is going to be returned to the SAML requester from SLO endpoint", error);
		LogoutResponseDocument errorResp = convertExceptionToResponse(localIssuer, 
				requestId, error);
		super.sendResponse(binding, errorResp, serviceUrl, relayState, response, "Logout Error");
	}

	public void sendRequest(Binding binding, LogoutRequestDocument requestDoc, String serviceUrl, 
			SAMLInternalLogoutContext context, HttpServletResponse response) throws IOException, EopException
	{
		super.sendRequest(binding, requestDoc, serviceUrl, context.getRelayState(), response, "Logout");
	}

	public void sendResponse(Binding binding, LogoutResponseDocument responseDoc, String serviceUrl, 
			SAMLExternalLogoutContext context, HttpServletResponse response) throws IOException, EopException
	{
		super.sendResponse(binding, responseDoc, serviceUrl, context.getRequestersRelayState(), 
				response, "Logout");
	}

	
	/**
	 * Creates a Base64 encoded SAML error response, which can be returned to the requester
	 * @param context
	 * @param error
	 * @return
	 */
	private LogoutResponseDocument convertExceptionToResponse(String issuer, String inReplyTo, 
			SAMLServerException error) 
	{
		
		LogoutResponse responseDoc = new LogoutResponse(
				new NameID(issuer, null).getXBean(), 
				inReplyTo, error);
		return responseDoc.getXMLBeanDoc();
	}

}
