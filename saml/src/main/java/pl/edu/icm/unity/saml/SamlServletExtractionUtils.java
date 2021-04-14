/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import pl.edu.icm.unity.base.utils.Log;

class SamlServletExtractionUtils 
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlServletExtractionUtils.class);
	
	static String extractFromPostBinding(String samlResponseEncoded, String what)
	{
		String samlResponse = new String(Base64.decode(samlResponseEncoded), StandardCharsets.UTF_8);
		if (log.isTraceEnabled())
			log.trace("Got SAML " + what + " using the HTTP POST binding:\n" + samlResponse);
		else
			log.debug("Got SAML " + what + " using the HTTP POST binding");
		return samlResponse;
	}
	
	static String extractFromRedirectBinding(String samlResponseEncoded, String what) throws IOException
	{
		String samlResponseDecoded = HttpRedirectBindingSupport.inflateSAMLRequest(samlResponseEncoded);
		if (log.isTraceEnabled())
			log.trace("Got SAML " + what + " using the HTTP Redirect binding:\n" + samlResponseDecoded);
		else
			log.debug("Got SAML " + what + " using the HTTP Redirect binding");
		return samlResponseDecoded;
	}
}
