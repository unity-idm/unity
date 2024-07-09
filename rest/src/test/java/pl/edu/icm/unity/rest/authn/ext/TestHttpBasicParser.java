/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn.ext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.nimbusds.jose.util.Base64;

import eu.unicore.security.HTTPAuthNTokens;
import pl.edu.icm.unity.base.utils.Log;

public class TestHttpBasicParser
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, TestHttpBasicParser.class);
	
	@Test
	public void shouldParseUrlEncodedCred() throws UnsupportedEncodingException
	{
		String authorizationHeader = toHeaderWithUrlEnc(":some user", "p @#$%^&*()-=,.");
		
		HTTPAuthNTokens tokens = HttpBasicParser.getHTTPCredentials(authorizationHeader, log, true);
		
		assertThat(tokens.getUserName(), is(":some user"));
		assertThat(tokens.getPasswd(), is("p @#$%^&*()-=,."));
	}

	@Test
	public void shouldParsePlainPassWithColon() throws UnsupportedEncodingException
	{
		String authorizationHeader = toHeaderPlain("some user", ":p ");
		
		HTTPAuthNTokens tokens = HttpBasicParser.getHTTPCredentials(authorizationHeader, log, false);
		
		assertThat(tokens.getUserName(), is("some user"));
		assertThat(tokens.getPasswd(), is(":p "));
	}

	@Test
	public void shouldParsePlainWithoutPass() throws UnsupportedEncodingException
	{
		String authorizationHeader = "Basic " + Base64.encode("some user".getBytes(HttpBasicParser.UTF8_CHARSET));
		
		HTTPAuthNTokens tokens = HttpBasicParser.getHTTPCredentials(authorizationHeader, log, false);
		
		assertThat(tokens.getUserName(), is("some user"));
		assertThat(tokens.getPasswd(), is(nullValue()));
	}

	@Test
	public void shouldParsePlainWithEmptyPass() throws UnsupportedEncodingException
	{
		String authorizationHeader = toHeaderPlain("some user", "");
		
		HTTPAuthNTokens tokens = HttpBasicParser.getHTTPCredentials(authorizationHeader, log, false);
		
		assertThat(tokens.getUserName(), is("some user"));
		assertThat(tokens.getPasswd(), is(""));
	}

	
	@Test
	public void shouldNotThrowErrorWhenNotParsableHeader()
	{
		String authorizationHeader = "Basic b2F1dGgtY2xpZW50Om9hdXRoLXBhc3M=,Bearer 5xtorQM7p_Vn5SYIfTqlXmUA8fYab0jxnoc-rofuJ5";
		try
		{
			HttpBasicParser.getHTTPCredentials(authorizationHeader, log, false);
		} catch (Exception e)
		{
			fail();
		}
	}
	
	private String toHeaderWithUrlEnc(String username, String pass) throws UnsupportedEncodingException
	{
		String src = URLEncoder.encode(username, HttpBasicParser.UTF8_CHARSET) + ":" 
				+ URLEncoder.encode(pass, HttpBasicParser.UTF8_CHARSET);
		return "Basic " + Base64.encode(src.getBytes(HttpBasicParser.UTF8_CHARSET));
	}

	private String toHeaderPlain(String username, String pass) throws UnsupportedEncodingException
	{
		String src = username + ":" + pass;
		return "Basic " + Base64.encode(src.getBytes(HttpBasicParser.UTF8_CHARSET));
	}
	
	
}
