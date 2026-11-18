/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_3;

import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.Constants;

class UpdateHelperTo4_3
{
	final static Set<String> oauthTokenTypes = Sets.newHashSet("oauth2Code", "oauth2Access", "oauth2Refresh",
			"usedOauth2Refresh");

	static Optional<ObjectNode> updateEffectiveScopesInOauthToken(ObjectNode objContent)
	{
		JsonNode scopes = objContent.get("effectiveScope");
		if (!isNonEmptyListOfScopeNames(scopes))
			return Optional.empty();
		ArrayNode newScopes = Constants.MAPPER.createArrayNode();
		for (JsonNode scope : scopes)
			newScopes.add(toRequestedScope(scope.asText()));
		objContent.replace("effectiveScope", newScopes);
		return Optional.of(objContent);
	}

	private static boolean isNonEmptyListOfScopeNames(JsonNode scopes)
	{
		if (scopes == null || !scopes.isArray() || scopes.isEmpty())
			return false;
		for (JsonNode scope : scopes)
			if (!scope.isTextual())
				return false;
		return true;
	}

	private static ObjectNode toRequestedScope(String scope)
	{
		ObjectNode scopeDef = Constants.MAPPER.createObjectNode();
		scopeDef.put("name", scope);
		scopeDef.put("description", "");
		scopeDef.put("pattern", false);
		scopeDef.putNull("attributes");

		ObjectNode scopeObj = Constants.MAPPER.createObjectNode();
		scopeObj.put("scope", scope);
		scopeObj.set("scopeDefinition", scopeDef);
		scopeObj.put("pattern", false);
		return scopeObj;
	}
}
