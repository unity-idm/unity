/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_0;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

@Component
public class JsonDumpUpdateFromV19 implements JsonDumpUpdate
{
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public int getUpdatedVersion()
	{
		return 19;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		JsonNode contents = root.get("contents");
		migrateHomeUiDisabledComponentsConfiguration(contents.withArray("endpointDefinition"));
		removeFidoIdentityFromForms(contents.withArray("registrationForm"));
		updateAuthenticatorIcons(contents);
		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	public void migrateHomeUiDisabledComponentsConfiguration(ArrayNode arrayNode)
	{
		for(JsonNode node : arrayNode)
		{
			Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration((ObjectNode) node.get("obj"));
			configuration.ifPresent(conf ->
				((ObjectNode) node.get("obj").get("configuration")).set("configuration", conf)
			);
		}
	}

	private void removeFidoIdentityFromForms(JsonNode tokensArray)
	{
		for (JsonNode formNode : tokensArray)
		{
			JsonNode obj = formNode.get("obj");
			JsonNode identityParams = obj.withArray("IdentityParams");
			ArrayNode filtered = Constants.MAPPER.createArrayNode();
			for (JsonNode paramNode : identityParams)
			{
				String identityType = paramNode.get("identityType").asText();
				if(!identityType.equals("fidoUserHandle"))
					filtered.add(paramNode);
			}
			((ObjectNode)obj).remove("IdentityParams");
			((ObjectNode)obj).putArray("IdentityParams").addAll(filtered);
		}
	}
	
	private void updateAuthenticatorIcons(JsonNode contents)
	{
		ArrayNode generics = (ArrayNode) contents.get("authenticator");
		if (generics == null)
			return;
		
		Iterator<JsonNode> elements = generics.elements();
		
		while (elements.hasNext())
		{
			ObjectNode next = (ObjectNode) elements.next();
			ObjectNode genericObject = (ObjectNode)next.get("obj");
			UpdateHelperTo4_0.updateOAuthAuthenticatorIcons(genericObject).ifPresent(o -> next.set("obj", o) );
		}
	}
}