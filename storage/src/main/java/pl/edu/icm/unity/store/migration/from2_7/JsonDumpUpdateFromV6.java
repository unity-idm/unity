/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_7;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.export.Update;

/**
 * Update JSon dump from V6 version, see {@link UpdateHelperFrom2_7}
 */
@Component
public class JsonDumpUpdateFromV6 implements Update
{
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		updateAuthenticators(contents);
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void updateAuthenticators(ObjectNode contents)
	{
		ArrayNode generics = (ArrayNode) contents.get("authenticator");
		if (generics == null)
			return;
		
		Iterator<JsonNode> elements = generics.elements();
		
		while (elements.hasNext())
		{
			ObjectNode next = (ObjectNode) elements.next();
			ObjectNode authenticator = (ObjectNode)next.get("obj");
			ObjectNode updated = UpdateHelperFrom2_7.updateAuthenticator(authenticator);
			next.set("obj", updated);
		}
	}
}
