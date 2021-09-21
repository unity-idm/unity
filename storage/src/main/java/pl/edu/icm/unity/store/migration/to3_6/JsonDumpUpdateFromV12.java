/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_6;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

/**
 * 2. changes the project management role attribute
 */
@Component
public class JsonDumpUpdateFromV12 implements JsonDumpUpdate
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV12.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 12;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);

		JsonNode contents = root.get("contents");

		updateOAuthRpScopes(contents.withArray("authenticator"));

		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));

	}

	private void updateOAuthRpScopes(ArrayNode authenticatorsArray)
	{
		for (int i = authenticatorsArray.size() - 1; i >= 0; i--)
		{
			JsonNode authenticator = authenticatorsArray.get(i);
			ObjectNode parsed = (ObjectNode) authenticator.get("obj");
			if ("oauth-rp".equals(parsed.get("verificationMethod").asText()))
			{
				JsonNode configuration = parsed.get("configuration");
				JsonNode migratedConfig = new OauthRpConfigurationMigrator(configuration).migrate();
				parsed.set("configuration", migratedConfig);
				LOG.info("Updating authenticator {}, \nold config: {}\nnew config: {}", 
						authenticator.get("name"), configuration, migratedConfig);
			}
		}
	}
}