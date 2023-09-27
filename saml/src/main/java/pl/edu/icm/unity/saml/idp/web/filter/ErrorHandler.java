/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLServerException;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.slo.SamlMessageHandler;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates an appropriate SAML error response. Additionally allows to show a plain error page, 
 * if SAML error can not be returned. 
 * 
 * @author K. Benedyczak
 */
public class ErrorHandler
{
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, ErrorHandler.class);
	private AttributeTypeSupport aTypeSupport;
	private final SamlMessageHandler messageHandler;
	private final FreemarkerAppHandler freemarker;
	private final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;

	public ErrorHandler(AttributeTypeSupport aTypeSupport, LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement,
			FreemarkerAppHandler freemarker)
	{
		this.aTypeSupport = aTypeSupport;
		this.freemarker = freemarker;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
		messageHandler = new SamlMessageHandler(freemarker);
	}
	
	/**
	 * Create an appropriate error response for the exception, and invokes freemarker view which automatically
	 * returns it to the SAML requester via web browser redirect.
	 */
	public void commitErrorResponse(SAMLAuthnContext samlCtx, 
			SAMLServerException error, 
			HttpServletResponse response) throws SAMLProcessingException, IOException, EopException
	{
		String serviceUrl = samlCtx.getSamlConfiguration().getReturnAddressForRequester(
					samlCtx.getRequest());
		if (serviceUrl == null)
			throw new SAMLProcessingException("No return URL in the SAML request. " +
					"Can't return the SAML error response.", error);

		log.warn("SAML error is going to be returned to the SAML requester by the IdP", error);
		
		AuthnResponseProcessor errorResponseProcessor = new AuthnResponseProcessor(aTypeSupport, lastAccessAttributeManagement, samlCtx);
		String encodedSamlError = processError(errorResponseProcessor, error);
		
		sendBackErrorResponse(error, serviceUrl, encodedSamlError, samlCtx.getRelayState(), response);
	}
	
	/**
	 * Creates a Base64 encoded SAML error response, which can be returned to the requester
	 */
	private String processError(AuthnResponseProcessor errorResponseProcessor, SAMLServerException error) 
	{
		ResponseDocument responseDoc = errorResponseProcessor.getErrorResponse(error);
		String assertion = responseDoc.xmlText();
		String encodedAssertion = Base64.getEncoder().encodeToString(assertion.getBytes(StandardCharsets.UTF_8));
		return encodedAssertion;
	}
	
	private void sendBackErrorResponse(SAMLServerException error, String serviceUrl, String encodedResponse,
			String relayState, HttpServletResponse response) throws SAMLProcessingException, 
			IOException, EopException
	{
		//security measure: if the request is invalid (usually not trusted) don't send the response,
		//as it may happen that the response URL is evil.
		SAMLConstants.SubStatus subStatus = error.getSamlSubErrorId();
		if (subStatus != null && subStatus.equals(SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED))
		{
			log.warn("Returning of an error response to the requester was blocked for security reasons."
					+ " Instead an error page should be presented.");
			throw new SAMLProcessingException(error);
		}
		Map<String, String> data = new HashMap<String, String>();
		data.put("SAMLResponse", encodedResponse);
		data.put("samlService", serviceUrl);
		data.put("samlError", error.getMessage());
		if (relayState != null)
			data.put("RelayState", relayState);
		
		response.setContentType("application/xhtml+xml; charset=utf-8");
		PrintWriter w = response.getWriter();
		freemarker.printGenericPage(w, "samlFinish.ftl", data);
		throw new EopException();
	}
	
	public void showErrorPage(SAMLProcessingException reason, HttpServletResponse response) 
			throws IOException, EopException
	{
		log.warn("SAML error is going to be shown to the user redirected to Unity IdP by the " +
				"SAML requester", reason);
		messageHandler.showError(reason, response);
	}
}
