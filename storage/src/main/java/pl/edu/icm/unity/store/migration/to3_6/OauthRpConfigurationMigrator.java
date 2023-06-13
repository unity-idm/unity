/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_6;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import pl.edu.icm.unity.base.exceptions.InternalException;

class OauthRpConfigurationMigrator
{
	private static final String SCOPES_PROPERTY = "unity.oauth2-rp.requiredScopes.";
	private final JsonNode configuration;

	OauthRpConfigurationMigrator(JsonNode configuration)
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
		String scopes = raw.getProperty(SCOPES_PROPERTY);
		raw.remove(SCOPES_PROPERTY);
		if (StringUtils.hasLength(scopes))
		{
			String[] scopesArray = scopes.split(" ");
			for (int idx = 0; idx < scopesArray.length; idx ++)
			{
				String scope = scopesArray[idx];
				//post 3.6 finding: the below line seems buggy (should be scope) - leaving for reference in case this 
				// causes issues
				if (StringUtils.hasLength(scopes))
				{
					raw.put(SCOPES_PROPERTY + (idx+1), scope.trim());
				}
			}
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
