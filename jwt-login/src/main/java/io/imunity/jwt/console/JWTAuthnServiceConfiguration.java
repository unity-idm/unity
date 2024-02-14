/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt.console;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.RESTEndpointProperties;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;

public class JWTAuthnServiceConfiguration
{
	private List<String> allowedCORSheaders;
	private List<String> allowedCORSorigins;
	private String credential;
	private int ttl;

	public JWTAuthnServiceConfiguration()
	{
		allowedCORSheaders = new ArrayList<>();
		allowedCORSorigins = new ArrayList<>();
		setTtl(JWTAuthenticationProperties.DEFAULT_TOKEN_TTL);
	}

	public List<String> getAllowedCORSheaders()
	{
		return allowedCORSheaders;
	}

	public void setAllowedCORSheaders(List<String> allowedCORSheaders)
	{
		this.allowedCORSheaders = allowedCORSheaders;
	}

	public void setAllowedCORSorigins(List<String> allowedCORSorigins)
	{
		this.allowedCORSorigins = allowedCORSorigins;
	}

	public List<String> getAllowedCORSorigins()
	{
		return allowedCORSorigins;
	}

	public String getCredential()
	{
		return credential;
	}

	public void setCredential(String credential)
	{
		this.credential = credential;
	}

	public int getTtl()
	{
		return ttl;
	}

	public void setTtl(int ttl)
	{
		this.ttl = ttl;
	}

	public String toProperties()
	{
		Properties raw = new Properties();

		getAllowedCORSheaders().forEach(a -> {

			int i = getAllowedCORSheaders().indexOf(a) + 1;
			raw.put(RESTEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_HEADERS + i, a);
		});

		getAllowedCORSorigins().forEach(a -> {

			int i = getAllowedCORSorigins().indexOf(a) + 1;
			raw.put(RESTEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_ORIGINS + i, a);
		});

		raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.SIGNING_CREDENTIAL,
				credential);
		raw.put(JWTAuthenticationProperties.PREFIX + JWTAuthenticationProperties.TOKEN_TTL,
				String.valueOf(ttl));

		RESTEndpointProperties prop = new RESTEndpointProperties(raw);
		return prop.getAsString();
	}

	public void fromProperties(String properties, MessageSource msg)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the rest admin service", e);
		}

		RESTEndpointProperties restAdminProp = new RESTEndpointProperties(raw);
		allowedCORSheaders = restAdminProp.getListOfValues(RESTEndpointProperties.ENABLED_CORS_HEADERS);
		allowedCORSorigins = restAdminProp.getListOfValues(RESTEndpointProperties.ENABLED_CORS_ORIGINS);

		JWTAuthenticationProperties jwtProp = new JWTAuthenticationProperties(raw);
		credential = jwtProp.getValue(JWTAuthenticationProperties.SIGNING_CREDENTIAL);
		ttl = jwtProp.getIntValue(JWTAuthenticationProperties.TOKEN_TTL);

	}
}