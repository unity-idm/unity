/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.translation;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestTranslationRule.Builder.class)
public class RestTranslationRule
{
	public final String condition;
	public final RestTranslationAction action;

	private RestTranslationRule(Builder builder)
	{
		this.condition = builder.condition;
		this.action = builder.action;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(action, condition);
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
		RestTranslationRule other = (RestTranslationRule) obj;
		return Objects.equals(action, other.action) && Objects.equals(condition, other.condition);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String condition;
		private RestTranslationAction action;

		private Builder()
		{
		}

		public Builder withCondition(String condition)
		{
			this.condition = condition;
			return this;
		}
		
		public Builder withAction(RestTranslationAction action)
		{
			this.action = action;
			return this;
		}

		public RestTranslationRule build()
		{
			return new RestTranslationRule(this);
		}
	}

}
