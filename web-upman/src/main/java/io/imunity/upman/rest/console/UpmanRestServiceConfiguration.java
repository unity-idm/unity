/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import io.imunity.upman.rest.UpmanRestEndpointProperties;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class UpmanRestServiceConfiguration

{
	private String rootGroup;
	private String authorizationGroup;

	public String toProperties()
	{
		Properties raw = new Properties();

		raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.ROOT_GROUP, rootGroup);
		raw.put(UpmanRestEndpointProperties.PREFIX + UpmanRestEndpointProperties.AUTHORIZATION_GROUP, authorizationGroup);

		UpmanRestEndpointProperties prop = new UpmanRestEndpointProperties(raw);
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

		UpmanRestEndpointProperties restAdminProp = new UpmanRestEndpointProperties(raw);
		rootGroup = restAdminProp.getValue(UpmanRestEndpointProperties.ROOT_GROUP);
		authorizationGroup = restAdminProp.getValue(UpmanRestEndpointProperties.AUTHORIZATION_GROUP);
	}

}