/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web.filter;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import eu.unicore.samly2.binding.SAMLMessageType;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.security.dsig.DSigException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.SamlSessionService;
import pl.edu.icm.unity.saml.slo.SamlMessageHandler;
import pl.edu.icm.unity.saml.slo.SamlRoutableMessage;
import pl.edu.icm.unity.saml.slo.SamlRoutableUnsignedMessage;
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
	
	public SSOResponseHandler(FreemarkerAppHandler freemarker)
	{
		messageHandler = new SamlMessageHandler(freemarker);
	}

	public void sendResponse(SamlRoutableMessage response, Binding binding,  
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
					throws IOException, EopException, DSigException
	{
		try
		{
			messageHandler.sendResponse(binding, response, httpResponse, "SSO Authentication response");
		} finally
		{
			cleanContext(httpRequest, false);
		}
	}
	
	public void handleException(AuthnResponseProcessor samlProcessor,
			Exception e, Binding binding, String serviceUrl, 
			String relayState, HttpServletRequest httpRequest, HttpServletResponse httpResponse,
			boolean invalidate) 
					throws EopException, IOException
	{
		SAMLServerException convertedException = samlProcessor.convert2SAMLError(e, null, true);
		ResponseDocument respDoc = samlProcessor.getErrorResponse(convertedException);
		SamlRoutableUnsignedMessage response = new SamlRoutableUnsignedMessage(respDoc,  
				SAMLMessageType.SAMLResponse, relayState, serviceUrl);
		log.warn("Sending SAML error to {} in effect of exception handling", serviceUrl, e);
		try
		{
			messageHandler.sendResponse(binding, response, httpResponse, 
					"SSO Authentication error response");
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
}
