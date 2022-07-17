/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_10;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import pl.edu.icm.unity.exceptions.InternalException;

class HomeEndpointConfigurationMigrator
{
	private static final String DISABLED_COMPONENTS_PREFIX = "unity.userhome.disabledComponents";
	private static final String OAUTH_TOKENS_COMPONENT_ID = "oauthTokens";
	private static final String PREFERENCES_COMPONENT_ID = "preferencesTab";

	private final JsonNode configuration;

	HomeEndpointConfigurationMigrator(JsonNode configuration)
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
		HashMap<String, String> toRemove = new HashMap<>();
		for (Object key : raw.keySet())
		{
			String sKey = (String) key;
			if (sKey.startsWith(DISABLED_COMPONENTS_PREFIX))
			{
				String value = raw.getProperty(sKey);
				if (value.equals(PREFERENCES_COMPONENT_ID) || value.equals(OAUTH_TOKENS_COMPONENT_ID))
				{
					toRemove.put(sKey, value);

				}
			}
		}

		toRemove.keySet().forEach(raw::remove);

		if (toRemove.values().containsAll(Set.of(PREFERENCES_COMPONENT_ID, OAUTH_TOKENS_COMPONENT_ID)))
		{
			raw.put(DISABLED_COMPONENTS_PREFIX + ".trustedApplicationTab" + raw.keySet().size(),
					"trustedApplicationTab");
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
			throw new InternalException("Invalid configuration of the HomeUI endpoint", e);
		}
		return raw;
	}
}
