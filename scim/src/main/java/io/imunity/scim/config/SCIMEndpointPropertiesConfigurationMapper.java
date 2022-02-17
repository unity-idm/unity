/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.scim.SCIMConstants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.RESTEndpointProperties;

public class SCIMEndpointPropertiesConfigurationMapper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMEndpointPropertiesConfigurationMapper.class);

	public static String toProperties(SCIMEndpointConfiguration configuration) throws JsonProcessingException
	{
		Properties rawRest = new Properties();
		configuration.allowedCorsHeaders
				.forEach(ach -> rawRest.put(SCIMEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_HEADERS
						+ (configuration.allowedCorsHeaders.indexOf(ach) + 1), ach));
		configuration.allowedCorsOrigins
				.forEach(aco -> rawRest.put(SCIMEndpointProperties.PREFIX + RESTEndpointProperties.ENABLED_CORS_ORIGINS
						+ (configuration.allowedCorsOrigins.indexOf(aco) + 1), aco));
		Properties rawScim = new Properties();
		configuration.membershipGroups.forEach(g -> rawScim.put(SCIMEndpointProperties.PREFIX
				+ SCIMEndpointProperties.MEMBERSHIP_GROUPS + (configuration.membershipGroups.indexOf(g) + 1), g));

		for (SchemaWithMapping s : configuration.schemas)
		{
			rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.SCHEMAS
					+ (configuration.schemas.indexOf(s) + 1), SCIMConstants.MAPPER.writeValueAsString(s));
		}

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
		List<SchemaWithMapping> schemas = new ArrayList<>();
		for (String schema : scimProp.getListOfValues(SCIMEndpointProperties.SCHEMAS))
		{
			try
			{
				schemas.add(SCIMConstants.MAPPER.readValue(schema, SchemaWithMapping.class));
			} catch (JsonProcessingException e)
			{
				log.error("Cannot read SCIM endpoint schema configuration", e);
			}
		}

		return SCIMEndpointConfiguration.builder()
				.withAllowedCorsHeaders(
						restEndpointProperties.getListOfValues(RESTEndpointProperties.ENABLED_CORS_HEADERS))
				.withAllowedCorsOrigins(
						restEndpointProperties.getListOfValues(RESTEndpointProperties.ENABLED_CORS_ORIGINS))
				.withSchemas(schemas)
				.withMembershipGroups(scimProp.getListOfValues(SCIMEndpointProperties.MEMBERSHIP_GROUPS))
				.withRootGroup(scimProp.getValue(SCIMEndpointProperties.ROOT_GROUP)).build();
	}

}
