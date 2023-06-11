/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_3;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Database update helper. If object is update then non empty optional is
 * return;
 * 
 * @author P.Piernik
 *
 */
public class UpdateHelperTo11
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperTo11.class);

	public static Optional<ObjectNode> replaceSidebarThemeWithUnityTheme(ObjectNode objContent)
	{
		String endpointName = objContent.get("name").asText(); 
		ObjectNode endpoint = (ObjectNode) objContent.get("configuration");
		String config = endpoint.get("configuration").asText();
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(config));
		} catch (IOException e)
		{
			throw new InternalException("Can not load endpoint configuration properties", e);
		}

		if (updateThemeProperty(raw, "defaultTheme", endpointName) || updateThemeProperty(raw, "mainTheme", endpointName)
				|| updateThemeProperty(raw, "authnTheme", endpointName))
		{
			endpoint.put("configuration", getAsString(raw));
			
			return Optional.of(objContent);
		}

		return Optional.empty();
	}

	private static boolean updateThemeProperty(Properties raw, String property, String endpointName)
	{
		String fullName =  "unity.endpoint.web." + property;
		if (raw.get(fullName) != null
				&& raw.get(fullName).equals("sidebarThemeValo"))
		{
			raw.put(fullName, "unityThemeValo");
			log.info("Settings " + endpointName  
				+ " endpoint configuration " + property + " theme to unityThemeValo");
			
			return true;
		}

		return false;
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
