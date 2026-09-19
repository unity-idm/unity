/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.export.JsonDumpUpdate;

@Component
public class JsonDumpUpdateFromV20 implements JsonDumpUpdate
{
	private final ObjectMapper objectMapper;

	JsonDumpUpdateFromV20(ObjectMapper objectMapper)
	{
		this.objectMapper = objectMapper;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 20;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		JsonNode contents = root.get("contents");
		migrateLocalOAuthAuthenticatorConfiguration(contents);
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private void migrateLocalOAuthAuthenticatorConfiguration(JsonNode contents)
	{
		ArrayNode generics = (ArrayNode) contents.get("authenticator");
		if (generics == null)
			return;
		
		Iterator<JsonNode> elements = generics.elements();
		
		while (elements.hasNext())
		{
			ObjectNode next = (ObjectNode) elements.next();
			ObjectNode genericObject = (ObjectNode)next.get("obj");
			UpdateHelperTo4_2.removeCredentialSettingFromLocalAuthenticator(genericObject)
					.ifPresent(o -> next.set("obj", o) );
		}
	}
}