/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor;
import pl.edu.icm.unity.server.utils.Log;
import xmlbeans.org.oasis.saml2.protocol.LogoutRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.LogoutResponseDocument;
import eu.unicore.samly2.webservice.SAMLLogoutInterface;

/**
 * Implementation of the SAML Single Logout protocol, SOAP binding.
 * @author K. Benedyczak
 */
public class SAMLSingleLogoutImpl implements SAMLLogoutInterface
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLSingleLogoutImpl.class);
	protected SAMLLogoutProcessor logoutProcessor;

	public SAMLSingleLogoutImpl(SAMLLogoutProcessor logoutProcessor)
	{
		this.logoutProcessor = logoutProcessor;
	}

	@Override
	public LogoutResponseDocument logoutRequest(LogoutRequestDocument reqDoc)
	{
		if (log.isTraceEnabled())
			log.trace("Received SAML Logout request: " + reqDoc.xmlText());

		LogoutResponseDocument respDoc = logoutProcessor.handleSynchronousLogoutFromSAML(reqDoc);
		
		if (log.isTraceEnabled())
			log.trace("Returning SAML Logout response: " + respDoc.xmlText());
		return respDoc;
	}
}
