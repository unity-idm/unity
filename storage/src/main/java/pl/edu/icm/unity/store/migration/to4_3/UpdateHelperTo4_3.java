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

public class UpdateHelperTo4_3
{
	public final static Set<String> oauthTokenTypes = Sets.newHashSet("oauth2Code", "oauth2Access", "oauth2Refresh",
			"usedOauth2Refresh");

	public static Optional<ObjectNode> fixOauthToken(ObjectNode objContent)
	{
		if (objContent.has("effectiveScope"))
		{
			ArrayNode scopesArray = (ArrayNode) objContent.get("effectiveScope");
			if (scopesArray.isEmpty())
				return Optional.empty();
			ArrayNode newScopes = Constants.MAPPER.createArrayNode();
			for (JsonNode s : scopesArray)
			{

				ObjectNode scopeDef = Constants.MAPPER.createObjectNode();
				scopeDef.put("name", s.asText());
				scopeDef.put("description", "");
				scopeDef.put("wildcard", false);
				scopeDef.putNull("attributes");
				
				ObjectNode scopeObj = Constants.MAPPER.createObjectNode();
				scopeObj.put("scope", s.asText());
				scopeObj.set("scopeDefinition", scopeDef);
				scopeObj.put("wildcard", false);
				newScopes.add(scopeObj);
			}

			objContent.replace("effectiveScope", newScopes);
			return Optional.of(objContent);
		}
		return Optional.empty();
	}
}
