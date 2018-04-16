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

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlObject;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.binding.HttpPostBindingSupport;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.exceptions.SAMLServerException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.webui.idpcommon.EopException;

/**
 * Base code for producing responses which are returned (some with the help of Freemarker) to the user's browser.
 * Some of the responses (low level errors) are shown as an error page. Other responses (high level errors 
 * and correct responses) are producing a web page which is redirecting the user to the final destination.
 * @author K. Benedyczak
 */
public abstract class ResponseHandlerBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ResponseHandlerBase.class);
	protected FreemarkerAppHandler freemarker;
	
	public ResponseHandlerBase(FreemarkerAppHandler freemarker)
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
		response.setContentType("text/html; charset=utf-8");
		PrintWriter w = response.getWriter();
		String errorMsg = error.getMessage();
		String cause = error.getCause() != null ? error.getCause().toString() : null;
		freemarker.printAppErrorPage(w, "SAML", "SAML IdP got an invalid request.", errorMsg, cause);
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
		freemarker.printGenericPage(w, "finishSaml.ftl", data);
		throw new EopException();
	}
	
	
	protected void sendRequest(Binding binding, XmlObject requestDoc, String serviceUrl, 
			String relayState, HttpServletResponse response, String info) 
					throws IOException, EopException
	{
		switch (binding)
		{
		case HTTP_POST:
			handlePostGeneric(requestDoc.xmlText(), info, SAMLMessageType.SAMLRequest, 
					serviceUrl, relayState, response);
			break;
		case HTTP_REDIRECT:
			handleRedirectGeneric(requestDoc.xmlText(), info, SAMLMessageType.SAMLRequest, 
					serviceUrl, relayState, response);
			break;
		default:
			throw new IllegalStateException("Unsupported binding: " + binding);
		}
	}

	protected void sendResponse(Binding binding, XmlObject responseDoc, String serviceUrl, 
			String relayState, HttpServletResponse response, String info) 
					throws IOException, EopException
	{
		switch (binding)
		{
		case HTTP_POST:
			handlePostGeneric(responseDoc.xmlText(), info, SAMLMessageType.SAMLResponse, 
					serviceUrl, relayState, response);
			break;
		case HTTP_REDIRECT:
			handleRedirectGeneric(responseDoc.xmlText(), info, SAMLMessageType.SAMLResponse, 
					serviceUrl, relayState, response);
			break;
		default:
			throw new IllegalStateException("Unsupported binding: " + binding);
		}
	}

	private void handleRedirectGeneric(String xml, String info, SAMLMessageType type, String serviceUrl, 
			String relayState, HttpServletResponse response) throws IOException, EopException
	{
		setCommonHeaders(response);
		log.debug("Returning " + info + " " + type + " with HTTP Redirect binding to " + serviceUrl);
		String redirectURL = HttpRedirectBindingSupport.getRedirectURL(type, 
				relayState, xml, serviceUrl);
		if (log.isTraceEnabled())
		{
			log.trace("SAML " + type + " is:\n" + xml);
			log.trace("Returned Redirect URL is:\n" + redirectURL);
		}
		response.sendRedirect(redirectURL);
		throw new EopException();
	}


	private void handlePostGeneric(String xml, String info, SAMLMessageType type, String serviceUrl, 
			String relayState, HttpServletResponse response) throws IOException, EopException
	{
		response.setContentType("text/html; charset=utf-8");
		setCommonHeaders(response);
		response.setDateHeader("Expires", -1);

		log.debug("Returning " + info + " " + type + " with HTTP POST binding to " + serviceUrl);
		String htmlResponse = HttpPostBindingSupport.getHtmlPOSTFormContents(
				type, serviceUrl, xml, relayState);
		if (log.isTraceEnabled())
		{
			log.trace("SAML " + info + " is:\n" + xml);
			log.trace("Returned POST form is:\n" + htmlResponse);
		}
		response.getWriter().append(htmlResponse);
		throw new EopException();
	}
	
	
	private void setCommonHeaders(HttpServletResponse response)
	{
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
	}
}
