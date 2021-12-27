/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_8;

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
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;

@Component
public class JsonDumpUpdateFromV14 implements JsonDumpUpdate
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV14.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 14;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		JsonNode contents = root.get("contents");
		updateOAuthEndpointRefreshTokenIssuePolicy(contents.withArray(EndpointHandler.ENDPOINT_OBJECT_TYPE));
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void updateOAuthEndpointRefreshTokenIssuePolicy(ArrayNode endpointsArray)
	{
		for (int i = endpointsArray.size() - 1; i >= 0; i--)
		{
			JsonNode endpoint = endpointsArray.get(i);
			ObjectNode parsed = (ObjectNode) endpoint.get("obj");
			String typeId = parsed.get("typeId").asText();	
			if ("OAuth2Authz".equals(typeId) || "OAuth2Token".equals(typeId))
			{
				ObjectNode configuration = (ObjectNode) parsed.get("configuration");
				JsonNode iconfiguration = configuration.get("configuration");
				JsonNode migratedConfig = new OAuthEndpointConfigurationMigrator(iconfiguration).migrate();
				configuration.set("configuration", migratedConfig);

				LOG.info("Updating OAuth endpoint {}, \nold config: {}\nnew config: {}", endpoint.get("name"),
						iconfiguration, migratedConfig);
			}
		}
	}
}