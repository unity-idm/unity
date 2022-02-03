/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.RESTEndpointProperties;

public class SCIMEndpointConfigurationMapper
{
	public static String toProperties(SCIMEndpointConfiguration configuration)
	{
		Properties rawRest = new Properties();

		configuration.allowedCorsHeaders.forEach(a ->
		{

			int i = configuration.allowedCorsHeaders.indexOf(a) + 1;
			rawRest.put(SCIMEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_HEADERS + i, a);
		});

		configuration.allowedCorsOrigins.forEach(a ->
		{

			int i = configuration.allowedCorsOrigins.indexOf(a) + 1;
			rawRest.put(SCIMEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_ORIGINS + i, a);
		});

		Properties rawScim = new Properties();

		configuration.membershipGroups.forEach(a ->
		{

			int i = configuration.membershipGroups.indexOf(a) + 1;
			rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.MEMBERSHIP_GROUPS + i, a);
		});

		rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.ROOT_GROUP, configuration.rootGroup);
		SCIMEndpointProperties propScim = new SCIMEndpointProperties(rawScim);
		RESTEndpointProperties propRest = new RESTEndpointProperties(rawRest);
		return propRest.getAsString() + "\n" + propScim.getAsString();
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
		RESTEndpointProperties restEndpointProperties = new RESTEndpointProperties(raw);
		return fromProperties(scimProp, restEndpointProperties);
	}

	public static SCIMEndpointConfiguration fromProperties(SCIMEndpointProperties scimProp,
			RESTEndpointProperties restEndpointProperties)
	{

		return SCIMEndpointConfiguration.builder()
				.withAllowedCorsHeaders(
						restEndpointProperties.getListOfValues(RESTEndpointProperties.ENABLED_CORS_HEADERS))
				.withAllowedCorsOrigins(
						restEndpointProperties.getListOfValues(RESTEndpointProperties.ENABLED_CORS_ORIGINS))
				.withMembershipGroups(scimProp.getListOfValues(SCIMEndpointProperties.MEMBERSHIP_GROUPS))
				.withRootGroup(scimProp.getValue(SCIMEndpointProperties.ROOT_GROUP)).build();

	}

}
