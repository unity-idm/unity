/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console.v8;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import io.imunity.upman.UpmanEndpointProperties;
import pl.edu.icm.unity.base.exceptions.InternalException;

/**
 * 
 * Contains upman service configuration
 * 
 * @author P.Piernik
 *
 */
public class UpmanServiceConfiguration
{

	private boolean enableHome;
	private String homeService;

	UpmanServiceConfiguration()
	{
	}

	public String toProperties()
	{
		Properties raw = new Properties();

		raw.put(UpmanEndpointProperties.PREFIX + UpmanEndpointProperties.ENABLE_HOME_LINK,
				String.valueOf(enableHome));

		if (isEnableHome())
		{
			if (getHomeService() != null && !getHomeService().isEmpty())
			{
				raw.put(UpmanEndpointProperties.PREFIX + UpmanEndpointProperties.HOME_ENDPOINT,
						getHomeService());
			}
		}

		UpmanEndpointProperties prop = new UpmanEndpointProperties(raw);
		return prop.getAsString();
	}

	public void fromProperties(String properties)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the upman ui service", e);
		}

		UpmanEndpointProperties upmanProperties = new UpmanEndpointProperties(raw);

		setEnableHome(upmanProperties.getBooleanValue(UpmanEndpointProperties.ENABLE_HOME_LINK));
		setHomeService(upmanProperties.getValue(UpmanEndpointProperties.HOME_ENDPOINT));

	}

	public boolean isEnableHome()
	{
		return enableHome;
	}

	public void setEnableHome(boolean enableHome)
	{
		this.enableHome = enableHome;
	}

	public String getHomeService()
	{
		return homeService;
	}

	public void setHomeService(String homeService)
	{
		this.homeService = homeService;
	}

}