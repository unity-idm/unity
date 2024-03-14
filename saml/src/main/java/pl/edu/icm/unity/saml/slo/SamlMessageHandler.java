/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.security.dsig.DSigException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import io.imunity.vaadin.endpoint.common.EopException;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base code for producing responses which are returned (some with the help of Freemarker) to the user's browser.
 * Some of the responses (low level errors) are shown as an error page. Other responses (high level errors 
 * and correct responses) are producing a web page which is redirecting the user to the final destination.
 */
public class SamlMessageHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlMessageHandler.class);
	protected FreemarkerAppHandler freemarker;
	
	public SamlMessageHandler(FreemarkerAppHandler freemarker)
	{
		this.freemarker = freemarker;
	}
	
	/**
	 * Shows a page with error.
	 */
	public void showError(Exception error, HttpServletResponse response) 
			throws IOException, EopException
	{
		response.setContentType("text/html; charset=utf-8");
		PrintWriter w = response.getWriter();
		String errorMsg = error.getMessage();
		String cause = error.getCause() != null ? error.getCause().toString() : null;
		freemarker.printAppErrorPage(w, "SAML", "SAML IdP got an invalid request.", errorMsg, cause);
		throw new EopException();
	}
	
	public void sendRequest(Binding binding, SamlRoutableMessage samlMessage, HttpServletResponse response, String logginName) 
					throws IOException, EopException, DSigException
	{
		switch (binding)
		{
		case HTTP_POST:
			handlePostGeneric(samlMessage, logginName, SAMLMessageType.SAMLRequest, response);
			break;
		case HTTP_REDIRECT:
			handleRedirectGeneric(samlMessage, logginName, SAMLMessageType.SAMLRequest, response);
			break;
		default:
			throw new IllegalStateException("Unsupported binding: " + binding);
		}
	}
	
	public void sendResponse(Binding binding, SamlRoutableMessage samlMessage, HttpServletResponse response, String loggingName) 
			throws IOException, EopException, DSigException
	{
		switch (binding)
		{
		case HTTP_POST:
			handlePostGeneric(samlMessage, loggingName, SAMLMessageType.SAMLResponse, response);
			break;
		case HTTP_REDIRECT:
			handleRedirectGeneric(samlMessage, loggingName, SAMLMessageType.SAMLResponse, response);
			break;
		default:
			throw new IllegalStateException("Unsupported binding: " + binding);
		}
	}
	
	private void handleRedirectGeneric(SamlRoutableMessage samlMessage, String info, SAMLMessageType type, 
			HttpServletResponse response) throws IOException, EopException, DSigException
	{
		setCommonHeaders(response);
		log.debug("Returning {} {} with HTTP Redirect binding to {}",
				info, type, samlMessage.getDestinationURL());
		String redirectURL = samlMessage.getRedirectURL();
		if (log.isTraceEnabled())
		{
			log.trace("SAML {} is:\n{}", type, samlMessage.getRawMessage());
			log.trace("Returned Redirect URL is:\n{}", redirectURL);
		}
		response.sendRedirect(redirectURL);
		throw new EopException();
	}


	private void handlePostGeneric(SamlRoutableMessage samlMessage, String info, SAMLMessageType type, 
			HttpServletResponse response) throws IOException, EopException, DSigException
	{
		response.setContentType("text/html; charset=utf-8");
		setCommonHeaders(response);
		response.setDateHeader("Expires", -1);

		log.debug("Returning {} {} with HTTP POST binding to {}", info, type, samlMessage.getDestinationURL());
		String htmlResponse = samlMessage.getPOSTConents();
		if (log.isTraceEnabled())
		{
			log.trace("SAML {} is:\n{}", info, samlMessage.getRawMessage());
			log.trace("Returned POST form is:\n{}", htmlResponse);
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
