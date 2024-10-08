/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.config;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.scim.SCIMConstants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;

public class SCIMEndpointPropertiesConfigurationMapper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMEndpointPropertiesConfigurationMapper.class);

	public static String toProperties(SCIMEndpointConfiguration configuration) throws JsonProcessingException
	{
		Properties rawScim = new Properties();
		configuration.allowedCorsHeaders
				.forEach(ach -> rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.ENABLED_CORS_HEADERS
						+ (configuration.allowedCorsHeaders.indexOf(ach) + 1), ach));
		configuration.allowedCorsOrigins
				.forEach(aco -> rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.ENABLED_CORS_ORIGINS
						+ (configuration.allowedCorsOrigins.indexOf(aco) + 1), aco));
		configuration.membershipGroups.forEach(g -> rawScim.put(SCIMEndpointProperties.PREFIX
				+ SCIMEndpointProperties.MEMBERSHIP_GROUPS + (configuration.membershipGroups.indexOf(g) + 1), g));
		configuration.excludedMembershipGroups.forEach(g -> rawScim.put(SCIMEndpointProperties.PREFIX
				+ SCIMEndpointProperties.EXCLUDED_MEMBERSHIP_GROUPS + (configuration.excludedMembershipGroups.indexOf(g) + 1), g));
		configuration.membershipAttributes.forEach(a -> rawScim.put(SCIMEndpointProperties.PREFIX
				+ SCIMEndpointProperties.MEMBERSHIP_ATTRIBUTES + (configuration.membershipAttributes.indexOf(a) + 1), a));
		for (SchemaWithMapping s : configuration.schemas)
		{
			rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.SCHEMAS
					+ (configuration.schemas.indexOf(s) + 1), SCIMConstants.MAPPER.writeValueAsString(s));
		}

		rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.ROOT_GROUP, configuration.rootGroup);
		if (configuration.restAdminGroup != null)
		{
			rawScim.put(SCIMEndpointProperties.PREFIX + SCIMEndpointProperties.REST_ADMIN_GROUP, configuration.restAdminGroup);
		}
		
		SCIMEndpointProperties propScim = new SCIMEndpointProperties(rawScim);
		return propScim.getAsString();
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
		List<SchemaWithMapping> schemas = new ArrayList<>();
	
		for (String schemaFile : scimProp.getListOfValues(SCIMEndpointProperties.SCHEMAS_FILE))
		{
			try
			{
				String source = FileUtils.readFileToString(new File(schemaFile), Charset.defaultCharset());
				schemas.add(SCIMConstants.MAPPER.readValue(source, SchemaWithMapping.class));
			} catch (JsonProcessingException e)
			{
				log.error("Cannot read SCIM endpoint schema configuration", e);
			} catch (IOException e)
			{
				log.error("Cannot read SCIM endpoint schema from file " + schemaFile, e);
			}
		}

		for (String schema : scimProp.getListOfValues(SCIMEndpointProperties.SCHEMAS))
		{
			try
			{
				SchemaWithMapping parsedSchema = SCIMConstants.MAPPER.readValue(schema, SchemaWithMapping.class);
				if (schemas.stream().filter(s -> s.id.equals(parsedSchema.id)).findFirst().isEmpty())
				{
					schemas.add(parsedSchema);
				}
			} catch (JsonProcessingException e)
			{
				log.error("Cannot read SCIM endpoint schema configuration", e);
			}
		}

		return SCIMEndpointConfiguration.builder()
				.withAllowedCorsHeaders(
						scimProp.getListOfValues(SCIMEndpointProperties.ENABLED_CORS_HEADERS))
				.withAllowedCorsOrigins(
						scimProp.getListOfValues(SCIMEndpointProperties.ENABLED_CORS_ORIGINS))
				.withSchemas(schemas)
				.withMembershipGroups(scimProp.getListOfValues(SCIMEndpointProperties.MEMBERSHIP_GROUPS))
				.withExcludedMembershipGroups(scimProp.getListOfValues(SCIMEndpointProperties.EXCLUDED_MEMBERSHIP_GROUPS))
				.withMembershipAttributes(scimProp.getListOfValues(SCIMEndpointProperties.MEMBERSHIP_ATTRIBUTES))
				.withRootGroup(scimProp.getValue(SCIMEndpointProperties.ROOT_GROUP))
				.withRestAdminGroup(scimProp.getValue(SCIMEndpointProperties.REST_ADMIN_GROUP))
				.build();
	}

}
