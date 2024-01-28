/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.console.v8;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.rest.RESTEndpointProperties;

/**
 * Contains REST admin service configuration
 * 
 * @author P.Piernik
 *
 */
public class RestAdminServiceConfiguration

{
	private List<String> allowedCORSheaders;
	private List<String> allowedCORSorigins;

	public RestAdminServiceConfiguration()
	{
		allowedCORSheaders = new ArrayList<>();
		allowedCORSorigins = new ArrayList<>();
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

	}

}