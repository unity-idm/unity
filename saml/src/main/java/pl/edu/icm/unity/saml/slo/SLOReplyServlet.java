/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import pl.edu.icm.unity.idpcommon.EopException;
import pl.edu.icm.unity.saml.SamlHttpServlet;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;

/**
 * Implements HTTP POST and HTTP Redirect bindings reception of SLO reply 
 * @author K. Benedyczak
 */
public class SLOReplyServlet extends SamlHttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOReplyServlet.class);

	private InternalLogoutProcessor logoutProcessor;
	
	public SLOReplyServlet(InternalLogoutProcessor logoutProcessor)
	{
		this.logoutProcessor = logoutProcessor;
	}

	@Override
	protected void postProcess(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlResponse, String relayState) throws IOException
	{
		try
		{
			LogoutResponseDocument respDoc = LogoutResponseDocument.Factory.parse(samlResponse);
			logoutProcessor.handleAsyncLogoutResponse(respDoc, relayState, resp);
		} catch (XmlException e)
		{
			log.warn("Got an invalid SAML Single Logout response (XML is broken)", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid SLO response (XML is malformed)");
		} catch (EopException e)
		{
			//ok
		}		
	}
}
