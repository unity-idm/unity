/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Represents a pair of authentication option with key.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class AuthenticationOptionKey
{
	private String authenticatorKey;
	private String optionKey;

	public AuthenticationOptionKey(String authenticatorKey, String optionKey)
	{
		this.authenticatorKey = authenticatorKey;
		this.optionKey = optionKey;
	}
	
	@JsonCreator
	public AuthenticationOptionKey(ObjectNode objectNode)
	{
		fromJson(objectNode);
	}

	public JsonNode toJsonObject()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("authenticatorKey", authenticatorKey);
		root.put("optionKey", optionKey);
		return root;
	}
	
	private void fromJson(ObjectNode root)
	{
		if (root == null || root.isNull())
			return;
		try
		{
			if (JsonUtil.notNull(root, "authenticatorKey"))
				this.authenticatorKey = root.get("authenticatorKey").asText();
			if (JsonUtil.notNull(root, "optionKey"))
				this.optionKey = root.get("optionKey").asText();
			
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize AuthenticationOptionKey from JSON", e);
		}
	}

	public static AuthenticationOptionKey valueOf(String globalKey)
	{
		return new AuthenticationOptionKey(
				AuthenticationOptionKeyUtils.decodeAuthenticator(globalKey), 
				AuthenticationOptionKeyUtils.decodeOption(globalKey)
		);
	}
	
	public String toGlobalKey()
	{
		return AuthenticationOptionKeyUtils.encode(authenticatorKey, optionKey);
	}
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof AuthenticationOptionKey))
			return false;
		AuthenticationOptionKey castOther = (AuthenticationOptionKey) other;
		return Objects.equals(authenticatorKey, castOther.authenticatorKey)
				&& Objects.equals(optionKey, castOther.optionKey);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authenticatorKey, optionKey);
	}
}
