/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.translation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestTranslationAction.Builder.class)
public class RestTranslationAction
{
	public final String name;
	public final List<String> parameters;

	private RestTranslationAction(Builder builder)
	{
		this.name = builder.name;
		this.parameters = Optional.ofNullable(builder.parameters)
				.map(Collections::unmodifiableList)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, parameters);
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
		RestTranslationAction other = (RestTranslationAction) obj;
		return Objects.equals(name, other.name) && Objects.equals(parameters, other.parameters);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private List<String> parameters = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withParameters(List<String> parameters)
		{
			this.parameters = Optional.ofNullable(parameters)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return this;
		}

		public RestTranslationAction build()
		{
			return new RestTranslationAction(this);
		}
	}

}
