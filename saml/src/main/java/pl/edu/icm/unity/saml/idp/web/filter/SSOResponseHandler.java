/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import eu.unicore.samly2.exceptions.SAMLServerException;
import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.web.ResponseHandlerBase;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Helper to send responses in SSO authn case, when working in non-Vaadin environment (plain servlets).
 * @author K. Benedyczak
 */
public class SSOResponseHandler extends ResponseHandlerBase
{
	public SSOResponseHandler(FreemarkerHandler freemarker)
	{
		super(freemarker);
	}

	public void sendResponse(Binding binding, ResponseDocument responseDoc, String serviceUrl, 
			String relayState, HttpServletRequest request, HttpServletResponse response) 
					throws IOException, EopException
	{
		try
		{
			super.sendResponse(binding, responseDoc, serviceUrl, relayState,
				response, "SSO Authentication response");
		} finally
		{
			cleanContext(request.getSession(), false);
		}
	}
	
	public void handleException(AuthnResponseProcessor samlProcessor,
			Exception e, Binding binding, String serviceUrl, 
			String relayState, HttpServletRequest request, HttpServletResponse response,
			 boolean invalidate) 
					throws EopException, IOException
	{
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);

		try
		{
			super.sendResponse(binding, respDoc, serviceUrl, relayState, response,
				"SSO Authentication error response");
		} finally
		{
			cleanContext(request.getSession(), invalidate);
		}
	}
	
	protected void cleanContext(HttpSession httpSession, boolean invalidate)
	{
		httpSession.removeAttribute(SamlParseServlet.SESSION_SAML_CONTEXT);
		if (invalidate)
			httpSession.invalidate();
	}
}
