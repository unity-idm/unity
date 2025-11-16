/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = Claim.Builder.class)
public record Claim(String name, List<JsonNode> values)
{
	public Claim
	{
		values = Optional.ofNullable(values) .map(List::copyOf) .orElse(null);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private List<JsonNode> values;

		public Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withValues(List<JsonNode> values)
		{
			this.values = values;
			return this;
		}

		public Claim build()
		{
			return new Claim(name, Optional.ofNullable(values)
					.map(List::copyOf)
					.orElse(null));
		}
	}
}
