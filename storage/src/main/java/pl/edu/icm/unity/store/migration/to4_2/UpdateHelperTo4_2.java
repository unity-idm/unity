/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.utils.Log;

public class UpdateHelperTo4_2
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, UpdateHelperTo4_2.class);

	static Optional<ObjectNode> removeCredentialSettingFromLocalAuthenticator(ObjectNode authenticator)
	{
		String type = authenticator.get("verificationMethod").asText();
		if (!type.equals("local-oauth-rp"))
			return Optional.empty();

		log.info("Remove credential definition from local OAuth authenticator {}",
				authenticator.get("name").asText());
		String configuration = authenticator.hasNonNull("configuration") ?
				authenticator.get("configuration").asText() : "";
		Properties properties = loadProperties(configuration);
		properties.remove("unity.oauth2-local-rp.credential");
		authenticator.put("configuration", getAsString(properties));
		return Optional.of(authenticator);
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
