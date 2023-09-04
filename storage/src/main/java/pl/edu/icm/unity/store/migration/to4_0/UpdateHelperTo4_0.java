/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_0;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class UpdateHelperTo4_0
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperTo4_0.class);

	public static Optional<TextNode> updateHomeUIConfiguration(ObjectNode node)
	{
		if (node.get("typeId").textValue().equals("UserHomeUI"))
		{
			String originalConfiguration = node.get("configuration").get("configuration").textValue();
			boolean edited = false;
			Properties appProps = loadProperties(originalConfiguration);
			Map<String, String> disabledComponents = appProps
					.entrySet().stream()
					.filter(entry -> entry.getKey().toString().contains("unity.userhome.disabledComponents."))
					.collect(Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().toString()));

			for (Map.Entry<String, String> entry : disabledComponents.entrySet())
			{
				String key = entry.getKey();
				String value = entry.getValue();
				if (value.equals("userInfo") || value.equals("identitiesManagement"))
				{
					appProps.remove(key);
					log.info("This row {} has been removed from endpoint configuration {}", value,
							node.get("name").textValue());
					edited = true;
				}
				if (value.equals("credentialTab"))
				{
					appProps.put("unity.userhome.disabledComponents.10", "trustedDevices");
					log.info("This row {} has been add to endpoint configuration {}", "trustedDevices",
							node.get("name").textValue());
					edited = true;
				}
			}
			if(edited)
				return Optional.of(new TextNode(getAsString(appProps)));
		}
		return Optional.empty();
	}

	private static Properties loadProperties(String originalConfiguration)
	{
		Properties appProps = new Properties();
		try
		{
			appProps.load(new ByteArrayInputStream(originalConfiguration.getBytes()));
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return appProps;
	}

	private static String getAsString(Properties properties)
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
}
