/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLServerException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Base code for producing responses which are returned with the help of Freemarker to the user's browser.
 * Some of the responses (low level errors) are shown as an error page. Other responses (ligh level errors 
 * and correct responses) are producing a web page which is redirecting the user to the final destination.
 * @author K. Benedyczak
 */
public abstract class ResponseHandlerBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ResponseHandlerBase.class);
	protected FreemarkerHandler freemarker;
	
	public ResponseHandlerBase(FreemarkerHandler freemarker)
	{
		this.freemarker = freemarker;
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
	protected void showError(Exception error, HttpServletResponse response) 
			throws IOException, EopException
	{
		response.setContentType("application/xhtml+xml; charset=utf-8");
		PrintWriter w = response.getWriter();
		Map<String, String> data = new HashMap<String, String>();
		data.put("error", error.getMessage());
		if (error.getCause() != null)
			data.put("errorCause", error.getCause().toString());
		freemarker.process("finishError.ftl", data, w);
		throw new EopException();
	}
	
	protected void sendBackErrorResponse(SAMLServerException error, String serviceUrl, String encodedResponse,
			String relayState, HttpServletResponse response) throws SAMLProcessingException, 
			IOException, EopException
	{
		//security measure: if the request is invalid (usually not trusted) don't send the response,
		//as it may happen that the response URL is evil.
		SAMLConstants.SubStatus subStatus = error.getSamlSubErrorId();
		if (subStatus != null && subStatus.equals(SAMLConstants.SubStatus.STATUS2_REQUEST_DENIED))
		{
			log.debug("Returning of an error response to the requester was blocked for security reasons."
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
		freemarker.process("finishSaml.ftl", data, w);
		throw new EopException();
	}
}
