/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ClaimsInTokenAttribute.Builder.class)
public class ClaimsInTokenAttribute
{
	static final String PARAMETER_NAME = "claims_in_tokens";

	public enum Value
	{
		id_token, token
	}

	public final Set<Value> values;

	private ClaimsInTokenAttribute(Builder builder)
	{
		this.values = builder.values;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Set<Value> values = Collections.emptySet();

		private Builder()
		{
		}

		public Builder withValues(Set<Value> values)
		{
			this.values = values;
			return this;
		}

		public ClaimsInTokenAttribute build()
		{
			return new ClaimsInTokenAttribute(this);
		}
	}

	
	
}
