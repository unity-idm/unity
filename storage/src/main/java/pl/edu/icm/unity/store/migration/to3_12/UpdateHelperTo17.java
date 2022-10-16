/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_12;

import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.Constants;

public class UpdateHelperTo17
{
	public final static Set<String> oauthTokenTypes = Sets.newHashSet("oauth2Code", "oauth2Access", "oauth2Refresh",
			"usedOauth2Refresh");

	public static Optional<ObjectNode> fixOauthToken(ObjectNode objContent)
	{
		if (objContent.has("audience"))
		{
			JsonNode jsonNode = objContent.get("audience");
			if (jsonNode.isArray())
				return Optional.empty();
			ArrayNode audience = Constants.MAPPER.createArrayNode();
			audience.add(jsonNode.asText());
			objContent.replace("audience", audience);
			return Optional.of(objContent);
		}
		return Optional.empty();
	}
}
