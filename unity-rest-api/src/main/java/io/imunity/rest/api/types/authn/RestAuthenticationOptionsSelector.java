/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api.types.authn;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

public class RestAuthenticationOptionsSelector
{
	public final String authenticatorKey;
	public final String optionKey;

	private RestAuthenticationOptionsSelector(Builder builder)
	{
		this.authenticatorKey = builder.authenticatorKey;
		this.optionKey = builder.optionKey;
	}

	@JsonCreator
	public RestAuthenticationOptionsSelector(JsonNode json)
	{
		if (json.isTextual())
		{
			String[] specs = json.asText()
					.split("\\.");
			if (specs.length != 2)
				throw new IllegalArgumentException("Invalid selector format: " + json.toString());
			this.authenticatorKey = specs[0];
			this.optionKey = specs[1];
		} else
		{
			if (!notNull(json, "authenticatorKey"))
				throw new IllegalArgumentException("Expecting authenticatorKey in json object: " + json.toString());
			if (!notNull(json, "optionKey"))
				throw new IllegalArgumentException("Expecting optionKey in json object: " + json.toString());

			this.authenticatorKey = getWithDef(json, "authenticatorKey", null);
			this.optionKey = getWithDef(json, "optionKey", null);
		}

	}

	private boolean notNull(JsonNode src, String name)
	{
		JsonNode jsonNode = src.get(name);
		return jsonNode != null && !jsonNode.isNull();
	}

	private String getWithDef(JsonNode src, String name, String def)
	{
		JsonNode n = src.get(name);
		return n != null ? (n.isNull() ? null : n.asText()) : def;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authenticatorKey, optionKey);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestAuthenticationOptionsSelector other = (RestAuthenticationOptionsSelector) obj;
		return Objects.equals(authenticatorKey, other.authenticatorKey) && Objects.equals(optionKey, other.optionKey);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String authenticatorKey;
		private String optionKey;

		private Builder()
		{
		}

		public Builder withAuthenticatorKey(String authenticatorKey)
		{
			this.authenticatorKey = authenticatorKey;
			return this;
		}

		public Builder withOptionKey(String optionKey)
		{
			this.optionKey = optionKey;
			return this;
		}

		public RestAuthenticationOptionsSelector build()
		{
			return new RestAuthenticationOptionsSelector(this);
		}
	}

}
