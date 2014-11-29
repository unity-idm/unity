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
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;

/**
 * Implements HTTP POST and HTTP Redirect bindings entry point to the SLO functionality.
 * @author K. Benedyczak
 */
public class SLOSAMLServlet extends SamlHttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SLOSAMLServlet.class);
	private SAMLLogoutProcessor logoutProcessor;

	public SLOSAMLServlet(SAMLLogoutProcessor logoutProcessor)
	{
		this.logoutProcessor = logoutProcessor;
	}

	@Override
	protected void postProcess(boolean isGet, HttpServletRequest req, HttpServletResponse resp,
			String samlResponse, String relayState) throws IOException
	{
		try
		{
			Binding binding = isGet ? Binding.HTTP_REDIRECT : Binding.HTTP_POST;
			LogoutRequestDocument reqDoc = LogoutRequestDocument.Factory.parse(samlResponse);
			logoutProcessor.handleAsyncLogoutFromSAML(reqDoc, relayState, resp, binding);
		} catch (XmlException e)
		{
			log.warn("Got a request to the SAML Single Logout endpoint, " +
					"with invalid request (XML is broken)", e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid SLO request (XML is malformed)");
			return;
		} catch (EopException e)
		{
			//ok
		}
	}
}
