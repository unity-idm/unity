/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_10;

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
public class JsonDumpUpdateFromV16 implements JsonDumpUpdate
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV16.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 16;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		JsonNode contents = root.get("contents");
		updateHomeEndpointDisabledComponents(contents.withArray(EndpointHandler.ENDPOINT_OBJECT_TYPE));
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void updateHomeEndpointDisabledComponents(ArrayNode endpointsArray)
	{
		for (int i = endpointsArray.size() - 1; i >= 0; i--)
		{
			JsonNode endpoint = endpointsArray.get(i);
			ObjectNode parsed = (ObjectNode) endpoint.get("obj");
			String typeId = parsed.get("typeId").asText();	
			if ("UserHomeUI".equals(typeId) )
			{
				ObjectNode configuration = (ObjectNode) parsed.get("configuration");
				JsonNode iconfiguration = configuration.get("configuration");
				JsonNode migratedConfig = new HomeEndpointConfigurationMigrator(iconfiguration).migrate();
				configuration.set("configuration", migratedConfig);

				LOG.info("Updating HomeUI endpoint {}, \nold config: {}\nnew config: {}", endpoint.get("name"),
						iconfiguration, migratedConfig);
			}
		}
	}
	
}