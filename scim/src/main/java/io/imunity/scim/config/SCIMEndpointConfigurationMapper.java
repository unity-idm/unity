/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import io.imunity.scim.SCIMEndpointProperties;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.RESTEndpointProperties;

public class SCIMEndpointConfigurationMapper
{
	public static String toProperties(SCIMEndpointConfiguration configuration)
	{
		Properties raw = new Properties();

		configuration.allowedCORSheaders.forEach(a ->
		{

			int i = configuration.allowedCORSheaders.indexOf(a) + 1;
			raw.put(SCIMEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_HEADERS + i, a);
		});

		configuration.allowedCORSorigins.forEach(a ->
		{

			int i = configuration.allowedCORSorigins.indexOf(a) + 1;
			raw.put(SCIMEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_ORIGINS + i, a);
		});

		configuration.membershipGroups.forEach(a ->
		{

			int i = configuration.membershipGroups.indexOf(a) + 1;
			raw.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.MEMBERSHIP_GROUPS + i, a);
		});

		raw.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.ROOT_GROUP, configuration.rootGroup);
		SCIMEndpointProperties prop = new SCIMEndpointProperties(raw);
		return prop.getAsString();
	}

	public static SCIMEndpointConfiguration fromProperties(String properties)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the scim service", e);
		}

		SCIMEndpointProperties scimProp = new SCIMEndpointProperties(raw);
		return fromProperties(scimProp);
	}

	public static SCIMEndpointConfiguration fromProperties(SCIMEndpointProperties scimProp)
	{

		return SCIMEndpointConfiguration.builder()
				.withAllowedCORSheaders(scimProp.getListOfValues(SCIMEndpointProperties.ENABLED_CORS_HEADERS))
				.withAllowedCORSorigins(scimProp.getListOfValues(SCIMEndpointProperties.ENABLED_CORS_ORIGINS))
				.withMembershipGroups(scimProp.getListOfValues(SCIMEndpointProperties.MEMBERSHIP_GROUPS))
				.withRootGroup(scimProp.getValue(SCIMEndpointProperties.ROOT_GROUP)).build();

	}

}
