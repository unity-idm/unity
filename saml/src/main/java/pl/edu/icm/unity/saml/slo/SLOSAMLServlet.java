/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.messages.RedirectedMessage;
import eu.unicore.samly2.messages.SAMLMessage;
import eu.unicore.samly2.messages.SAMLVerifiableElement;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.saml.SamlHttpRequestServlet;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;

/**
 * Implements HTTP POST and HTTP Redirect bindings entry point to the SLO functionality.
 * @author K. Benedyczak
 */
public class SLOSAMLServlet extends SamlHttpRequestServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOSAMLServlet.class);
	private SAMLLogoutProcessor logoutProcessor;

	public SLOSAMLServlet(SAMLLogoutProcessor logoutProcessor)
	{
		super(false);
		this.logoutProcessor = logoutProcessor;
	}

	@Override
	protected void postProcessRequest(boolean isGet, HttpServletRequest httpReq, HttpServletResponse httpResp,
			String samlRequest, String relayState) throws IOException
	{
		try
		{
			SAMLBindings binding = isGet ? SAMLBindings.HTTP_REDIRECT : SAMLBindings.HTTP_POST;
			LogoutRequestDocument reqDoc = LogoutRequestDocument.Factory.parse(samlRequest);
			SAMLVerifiableElement verifiableMessage = binding == SAMLBindings.HTTP_REDIRECT ? 
					new RedirectedMessage(httpReq.getQueryString()) 
					: new XMLExpandedMessage(reqDoc, reqDoc.getLogoutRequest());
			SAMLMessage<LogoutRequestDocument> requestMessage = new SAMLMessage<>(
					verifiableMessage, relayState, binding, reqDoc);
			logoutProcessor.handleAsyncLogoutFromSAML(requestMessage, httpResp);
		} catch (XmlException e)
		{
			log.warn("Got a request to the SAML Single Logout endpoint, " +
					"with invalid request (XML is broken)", e);
			httpResp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid SLO request (XML is malformed)");
			return;
		} catch (EopException e)
		{
			//ok
		}
	}
}
