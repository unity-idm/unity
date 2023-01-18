/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.layout;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestFormLayout.Builder.class)
public class RestFormLayout
{
	public final List<RestFormElement> elements;

	private RestFormLayout(Builder builder)
	{
		this.elements = Optional.ofNullable(builder.elements)
				.map(List::copyOf)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(elements);
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
		RestFormLayout other = (RestFormLayout) obj;
		return Objects.equals(elements, other.elements);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<RestFormElement> elements = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withElements(List<RestFormElement> elements)
		{
			this.elements = Optional.ofNullable(elements)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public RestFormLayout build()
		{
			return new RestFormLayout(this);
		}
	}

}
