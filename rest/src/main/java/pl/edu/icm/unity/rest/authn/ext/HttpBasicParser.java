/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.impl.util.Base64;

import eu.unicore.security.HTTPAuthNTokens;

/**
 * HTTP Basic authn parsing. Supports plain and URL encoded variants.
 * 
 * @author K. Benedyczak
 */
public class HttpBasicParser
{
	static final String UTF8_CHARSET = Charset.forName("UTF-8").name();
	
	static HTTPAuthNTokens getHTTPCredentials(String authorizationHeader, Logger log, boolean urlEncoded)
	{
		if (authorizationHeader == null)
			return null;
		if (!authorizationHeader.startsWith("Basic "))
			return null;
		
		String encoded = authorizationHeader.substring(6);
		String decoded = new String(Base64.decode(encoded.getBytes(StandardCharsets.US_ASCII)),
				StandardCharsets.US_ASCII);
		if (decoded.isEmpty())
		{
			log.warn("Ignoring malformed Authorization HTTP header element" +
					" (empty string after decode)");
			return null;
		}

		int colon = decoded.indexOf(':');
		String[] split;
		if (colon == -1)
		{
			split = new String[]{decoded};
		} else 
		{
			if (colon < decoded.length()-1)
				split = new String[]{decoded.substring(0, colon), decoded.substring(colon+1)};
			else
				split = new String[]{decoded.substring(0, colon), ""};
		}
		
		String username = urlDecodeIfNeeded(split[0], urlEncoded);
		String secret = split.length == 1 ? null : urlDecodeIfNeeded(split[1], urlEncoded);
		return new HTTPAuthNTokens(username, secret);
	}
	
	private static String urlDecodeIfNeeded(String value, boolean urlEncoded)
	{
		if (value == null || !urlEncoded)
			return value;
		try
		{
			return URLDecoder.decode(value, UTF8_CHARSET);
		} catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException("UTF 8 is unsupported??", e);
		}
	}
}
