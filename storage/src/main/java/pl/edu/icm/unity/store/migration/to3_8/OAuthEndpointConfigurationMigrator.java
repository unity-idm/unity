/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_8;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import pl.edu.icm.unity.exceptions.InternalException;

class OAuthEndpointConfigurationMigrator
{
	private static final String REFRESH_TOKEN_VALIDITY_PROPERTY = "unity.oauth2.as.refreshTokenValidity";
	private static final String REFRESH_TOKEN_ISSUE_POLICY_PROPERTY = "unity.oauth2.as.refreshTokenIssuePolicy";

	private final JsonNode configuration;

	OAuthEndpointConfigurationMigrator(JsonNode configuration)
	{
		this.configuration = configuration;
	}

	JsonNode migrate()
	{
		String config = configuration.asText();
		String migratedConfig = migrateConfiguration(config);
		return TextNode.valueOf(migratedConfig);
	}

	private static String migrateConfiguration(String configuration)
	{
		Properties raw = parse(configuration);
		String refreshTokenValidity = raw.getProperty(REFRESH_TOKEN_VALIDITY_PROPERTY);
		if (StringUtils.hasLength(refreshTokenValidity))
		{
			int refreshTokenValidityAsInt = Integer.valueOf(refreshTokenValidity);
			if (refreshTokenValidityAsInt >= 0)
			{
				raw.put(REFRESH_TOKEN_ISSUE_POLICY_PROPERTY, "ALWAYS");
			} else if (refreshTokenValidityAsInt < 0)
			{
				raw.put(REFRESH_TOKEN_ISSUE_POLICY_PROPERTY, "NEVER");
				raw.remove(REFRESH_TOKEN_VALIDITY_PROPERTY);
			}
		} else
		{
			raw.put(REFRESH_TOKEN_ISSUE_POLICY_PROPERTY, "NEVER");
			raw.remove(REFRESH_TOKEN_VALIDITY_PROPERTY);
		}

		return getAsString(raw);
	}

	public static String getAsString(Properties properties)
	{
		StringWriter writer = new StringWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new InternalException("Can not save properties to string");
		}
		return writer.getBuffer().toString();
	}

	private static Properties parse(String source)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(source));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth-rp verificator", e);
		}
		return raw;
	}
}
