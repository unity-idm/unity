/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SAMLProcessingException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.web.ResponseHandlerBase;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;
import eu.unicore.samly2.binding.HttpPostBindingSupport;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.LogoutResponse;

/**
 * Prepares and return LogoutResponses for the asynchronous bindings. 
 * Also support for showing an error page directly is provided with freemarker help.
 * 
 * @author K. Benedyczak
 */
public class SLOAsyncResponseHandler extends ResponseHandlerBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOAsyncResponseHandler.class);
	
	public SLOAsyncResponseHandler(FreemarkerHandler freemarker)
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
		
		switch (binding)
		{
		case HTTP_POST:
			handlePostGeneric(errorResp.xmlText(), "Logout Error", SAMLMessageType.SAMLResponse, 
					serviceUrl, relayState, response);
			break;
		case HTTP_REDIRECT:
			handleRedirectGeneric(errorResp.xmlText(), "Logout Error", SAMLMessageType.SAMLResponse, 
					serviceUrl, relayState, response);
			break;
		default:
			throw new IllegalStateException("Unsupported binding: " + binding);
		}
	}

	public void sendRequest(Binding binding, LogoutRequestDocument requestDoc, String serviceUrl, 
			SAMLInternalLogoutContext context, HttpServletResponse response) throws IOException, EopException
	{
		switch (binding)
		{
		case HTTP_POST:
			handlePostGeneric(requestDoc.xmlText(), "Logout", SAMLMessageType.SAMLRequest, 
					serviceUrl, context.getRelayState(), response);
			break;
		case HTTP_REDIRECT:
			handleRedirectGeneric(requestDoc.xmlText(), "Logout", SAMLMessageType.SAMLRequest, 
					serviceUrl, context.getRelayState(), response);
			break;
		default:
			throw new IllegalStateException("Unsupported binding: " + binding);
		}
	}

	public void sendResponse(Binding binding, LogoutResponseDocument responseDoc, String serviceUrl, 
			SAMLExternalLogoutContext context, HttpServletResponse response) throws IOException, EopException
	{
		switch (binding)
		{
		case HTTP_POST:
			handlePostGeneric(responseDoc.xmlText(), "Logout", SAMLMessageType.SAMLResponse, 
					serviceUrl, context.getRequestersRelayState(), response);
			break;
		case HTTP_REDIRECT:
			handleRedirectGeneric(responseDoc.xmlText(), "Logout", SAMLMessageType.SAMLResponse, 
					serviceUrl, context.getRequestersRelayState(), response);
			break;
		default:
			throw new IllegalStateException("Unsupported binding: " + binding);
		}

	}

	protected void handleRedirectGeneric(String xml, String info, SAMLMessageType type, String serviceUrl, 
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


	protected void handlePostGeneric(String xml, String info, SAMLMessageType type, String serviceUrl, 
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
	
	protected void setCommonHeaders(HttpServletResponse response)
	{
		response.setHeader("Cache-Control","no-cache,no-store");
		response.setHeader("Pragma","no-cache");
	}
}
