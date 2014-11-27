/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.io.IOException;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.samly2.binding.HttpRedirectBindingSupport;

/**
 * Generic SAML servlet. It provides support for low level parsing of both HTTP Redirect and HTTP POST bindings
 * supporting RelayState and both request and response parsing. Good foundation for extensions.
 * 
 * @author K. Benedyczak
 */
public class SamlHttpServlet extends HttpServlet
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SamlHttpServlet.class);
	
	protected String extractResponseFromPostBinding(String samlResponseEncoded)
	{
		return extractFromPostBinding(samlResponseEncoded, "response");
	}
	
	protected String extractRequestFromPostBinding(String samlResponseEncoded)
	{
		return extractFromPostBinding(samlResponseEncoded, "request");
	}
	
	protected String extractFromPostBinding(String samlResponseEncoded, String what)
	{
		String samlResponse = new String(Base64.decode(samlResponseEncoded));
		if (log.isTraceEnabled())
			log.trace("Got SAML " + what + " using the HTTP POST binding:\n" + samlResponse);
		else
			log.debug("Got SAML " + what + " using the HTTP POST binding");
		return samlResponse;
	}
	
	protected String extractResponseFromRedirectBinding(String samlResponseEncoded) throws IOException
	{
		return extractFromRedirectBinding(samlResponseEncoded, "response");
	}

	protected String extractRequestFromRedirectBinding(String samlResponseEncoded) throws IOException
	{
		return extractFromRedirectBinding(samlResponseEncoded, "request");
	}
	
	protected String extractFromRedirectBinding(String samlResponseEncoded, String what) throws IOException
	{
		String samlResponseDecoded = HttpRedirectBindingSupport.inflateSAMLRequest(samlResponseEncoded);
		if (log.isTraceEnabled())
			log.trace("Got SAML " + what + " using the HTTP Redirect binding:\n" + samlResponseDecoded);
		else
			log.debug("Got SAML " + what + " using the HTTP Redirect binding");
		return samlResponseDecoded;
	}
}
