/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.authn.RestAuthenticationOptionsSelector;
import java.util.Collections;

@JsonDeserialize(builder = RestExternalSignupSpec.Builder.class)
public class RestExternalSignupSpec
{
	public final List<RestAuthenticationOptionsSelector> specs;

	private RestExternalSignupSpec(Builder builder)
	{
		this.specs = Optional.ofNullable(builder.specs)
				.map(List::copyOf)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(specs);
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
		RestExternalSignupSpec other = (RestExternalSignupSpec) obj;
		return Objects.equals(specs, other.specs);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<RestAuthenticationOptionsSelector> specs = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withSpecs(List<RestAuthenticationOptionsSelector> specs)
		{
			this.specs = Optional.ofNullable(specs)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public RestExternalSignupSpec build()
		{
			return new RestExternalSignupSpec(this);
		}
	}

}
