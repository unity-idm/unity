/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;

public class PrivateKeyJwtExtraInfo
{
	private String jwks;

	private PrivateKeyJwtExtraInfo()
	{
	}

	public PrivateKeyJwtExtraInfo(String jwks)
	{
		this.jwks = jwks;
	}

	public String getJwks()
	{
		return jwks;
	}

	public static PrivateKeyJwtExtraInfo fromJson(String json)
	{
		PrivateKeyJwtExtraInfo ret = new PrivateKeyJwtExtraInfo();
		if (json == null || json.isBlank())
			return ret;
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			if (root.has("jwks"))
				ret.jwks = root.get("jwks").asText(null);
			return ret;
		} catch (IOException e)
		{
			throw new InternalException("Can't deserialize private_key_jwt extra credential information from JSON", e);
		}
	}

	public String toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		if (jwks != null)
			root.put("jwks", jwks);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize private_key_jwt extra credential information to JSON", e);
		}
	}
}
