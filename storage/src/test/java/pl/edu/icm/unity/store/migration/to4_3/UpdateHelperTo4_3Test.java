/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;

public class UpdateHelperTo4_3Test
{

	@Test
	void testFixOauthTokenWithScopes()
	{
		ObjectNode input = Constants.MAPPER.createObjectNode();
		ArrayNode scopes = Constants.MAPPER.createArrayNode();
		scopes.add("read");
		scopes.add("write");
		input.set("effectiveScope", scopes);

		Optional<ObjectNode> resultOpt = UpdateHelperTo4_3.fixOauthToken(input);

		assertTrue(resultOpt.isPresent());
		ObjectNode result = resultOpt.get();

		ArrayNode effectiveScope = (ArrayNode) result.get("effectiveScope");
		assertEquals(2, effectiveScope.size());

		// Check first scope
		ObjectNode first = (ObjectNode) effectiveScope.get(0);
		assertEquals("read", first.get("scope")
				.asText());
		assertFalse(first.get("wildcard")
				.asBoolean());
		ObjectNode firstDef = (ObjectNode) first.get("scopeDefinition");
		assertEquals("read", firstDef.get("name")
				.asText());
		assertEquals("", firstDef.get("description")
				.asText());
		assertFalse(firstDef.get("wildcard")
				.asBoolean());

		// Check second scope
		ObjectNode second = (ObjectNode) effectiveScope.get(1);
		assertEquals("write", second.get("scope")
				.asText());
	}

	@Test
	void testFixOauthTokenWithEmptyScopes()
	{
		ObjectNode input = Constants.MAPPER.createObjectNode();
		ArrayNode scopes = Constants.MAPPER.createArrayNode();
		input.set("effectiveScope", scopes);

		Optional<ObjectNode> resultOpt = UpdateHelperTo4_3.fixOauthToken(input);

		assertFalse(resultOpt.isPresent());
	}

	@Test
	void testFixOauthTokenWithoutEffectiveScope()
	{
		ObjectNode input = Constants.MAPPER.createObjectNode();
		input.put("someOtherField", "value");

		Optional<ObjectNode> resultOpt = UpdateHelperTo4_3.fixOauthToken(input);

		assertFalse(resultOpt.isPresent());
	}
}
