/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.messages.RedirectedMessage;
import eu.unicore.samly2.messages.SAMLMessage;
import eu.unicore.samly2.messages.SAMLVerifiableElement;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.saml.SamlHttpResponseServlet;
import io.imunity.vaadin.endpoint.common.EopException;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;

/**
 * Implements HTTP POST and HTTP Redirect bindings reception of SLO reply 
 */
public class SLOReplyServlet extends SamlHttpResponseServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOReplyServlet.class);

	private InternalLogoutProcessor logoutProcessor;
	
	public SLOReplyServlet(InternalLogoutProcessor logoutProcessor)
	{
		super(true);
		this.logoutProcessor = logoutProcessor;
	}

	@Override
	protected void postProcessResponse(boolean isGet, HttpServletRequest httpReq, HttpServletResponse httpResp,
			String samlResponse, String relayState) throws IOException
	{
		try
		{
			SAMLBindings binding = isGet ? SAMLBindings.HTTP_REDIRECT : SAMLBindings.HTTP_POST;
			LogoutResponseDocument respDoc = LogoutResponseDocument.Factory.parse(samlResponse);
			SAMLVerifiableElement verifiableMessage = binding == SAMLBindings.HTTP_REDIRECT ? 
					new RedirectedMessage(httpReq.getQueryString()) 
					: new XMLExpandedMessage(respDoc, respDoc.getLogoutResponse());
			SAMLMessage<LogoutResponseDocument> responseMessage = new SAMLMessage<>(
					verifiableMessage, relayState, binding, respDoc);
			logoutProcessor.handleAsyncLogoutResponse(responseMessage, httpResp);
		} catch (XmlException e)
		{
			log.warn("Got an invalid SAML Single Logout response (XML is broken)", e);
			httpResp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid SLO response (XML is malformed)");
		} catch (EopException e)
		{
			//ok
		}		
	}
}
