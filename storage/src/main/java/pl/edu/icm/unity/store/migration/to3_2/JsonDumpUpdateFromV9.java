/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.export.Update;

/**
 * Drops extracted attributes from identity types.
 */
@Component
public class JsonDumpUpdateFromV9 implements Update
{
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		dropAttributesExtractionFlag(contents);
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void dropAttributesExtractionFlag(ObjectNode contents)
	{
		ArrayNode identities = contents.withArray("identityTypes");
		for (JsonNode identityType: identities)
			((ObjectNode)identityType).remove("extractedAttributes");
	}
}