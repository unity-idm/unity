/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;

@JsonDeserialize(builder = DBExternalSignupSpec.Builder.class)
class DBExternalSignupSpec
{
	public final List<DBAuthenticationOptionsSelector> specs;

	private DBExternalSignupSpec(Builder builder)
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
		DBExternalSignupSpec other = (DBExternalSignupSpec) obj;
		return Objects.equals(specs, other.specs);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<DBAuthenticationOptionsSelector> specs = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withSpecs(List<DBAuthenticationOptionsSelector> specs)
		{
			this.specs = Optional.ofNullable(specs)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public DBExternalSignupSpec build()
		{
			return new DBExternalSignupSpec(this);
		}
	}

}
