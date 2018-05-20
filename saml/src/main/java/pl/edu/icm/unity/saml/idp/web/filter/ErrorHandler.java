/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import eu.unicore.samly2.exceptions.SAMLServerException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.web.ResponseHandlerBase;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Creates an appropriate SAML error response. Additionally allows to show a plain error page, 
 * if SAML error can not be returned. 
 * 
 * @author K. Benedyczak
 */
public class ErrorHandler extends ResponseHandlerBase
{
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, ErrorHandler.class);
	private AttributeTypeSupport aTypeSupport;

	public ErrorHandler(AttributeTypeSupport aTypeSupport, FreemarkerAppHandler freemarker)
	{
		super(freemarker);
		this.aTypeSupport = aTypeSupport;
	}


	/**
	 * Creates a Base64 encoded SAML error response, which can be returned to the requester
	 * @param samlCtx
	 * @param error
	 * @return
	 * @throws SAMLProcessingException
	 */
	private String processError(AuthnResponseProcessor errorResponseProcessor, SAMLServerException error) 
	{
		ResponseDocument responseDoc = errorResponseProcessor.getErrorResponse(error);
		String assertion = responseDoc.xmlText();
		String encodedAssertion = Base64.encode(assertion.getBytes(StandardCharsets.UTF_8));
		return encodedAssertion;
	}
	
	/**
	 * Create an appropriate error response for the exception, and invokes freemarker view which automatically
	 * returns it to the SAML requester via web browser redirect.
	 * @param samlCtx
	 * @param error
	 * @param response
	 * @throws SAMLProcessingException
	 * @throws IOException
	 * @throws EopException 
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

		log.debug("SAML error is going to be returned to the SAML requester by the IdP", error);
		
		AuthnResponseProcessor errorResponseProcessor = new AuthnResponseProcessor(aTypeSupport, samlCtx);
		String encodedSamlError = processError(errorResponseProcessor, error);
		
		sendBackErrorResponse(error, serviceUrl, encodedSamlError, samlCtx.getRelayState(), response);
	}
	
	public void showErrorPage(SAMLProcessingException reason, HttpServletResponse response) 
			throws IOException, EopException
	{
		log.debug("SAML error is going to be shown to the user redirected to Unity IdP by the " +
				"SAML requester", reason);
		super.showError(reason, response);
	}
}
