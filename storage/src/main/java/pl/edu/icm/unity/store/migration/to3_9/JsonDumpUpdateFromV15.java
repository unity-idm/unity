/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_9;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

@Component
public class JsonDumpUpdateFromV15 implements JsonDumpUpdate
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, JsonDumpUpdateFromV15.class);

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 15;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		JsonNode contents = root.get("contents");
		udpateStringSyntaxAttributeTypes(contents.withArray("attributeTypes"));
		
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}
	
	private void udpateStringSyntaxAttributeTypes(JsonNode attributeTypesArray)
	{
		for (JsonNode attrTypeNode : attributeTypesArray)
		{
			ObjectNode attrTypeObj = (ObjectNode) attrTypeNode;
			String syntax = attrTypeObj.get("syntaxId").asText();
			if (syntax.equals("string"))
			{
				
				JsonNode jsonNode = attrTypeObj.get("syntaxState");
				if (jsonNode ==null || jsonNode.isNull())
					continue;
				ObjectNode syntaxConfig = (ObjectNode) jsonNode;
				if (syntaxConfig.get("maxLength").asInt() > 1000)
				{
					syntaxConfig.put("editWithTextArea", "true");
				} else
				{
					syntaxConfig.put("editWithTextArea", "false");
				}

				LOG.info("Updating attribute type {}, set editWithTextArea={} in string syntax",
						attrTypeObj.get("name").asText(), syntaxConfig.get("editWithTextArea"));
			}
		}
	}
}