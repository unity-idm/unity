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

@JsonDeserialize(builder = DBExternalSignupGridSpec.Builder.class)
class DBExternalSignupGridSpec
{
	final List<DBAuthenticationOptionsSelector> specs;
	final DBAuthnGridSettings gridSettings;

	private DBExternalSignupGridSpec(Builder builder)
	{
		this.specs = Optional.ofNullable(builder.specs)
				.map(List::copyOf)
				.orElse(null);
		this.gridSettings = builder.gridSettings;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(gridSettings, specs);
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
		DBExternalSignupGridSpec other = (DBExternalSignupGridSpec) obj;
		return Objects.equals(gridSettings, other.gridSettings) && Objects.equals(specs, other.specs);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<DBAuthenticationOptionsSelector> specs = Collections.emptyList();
		private DBAuthnGridSettings gridSettings;

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

		public Builder withGridSettings(DBAuthnGridSettings gridSettings)
		{
			this.gridSettings = gridSettings;
			return this;
		}

		public DBExternalSignupGridSpec build()
		{
			return new DBExternalSignupGridSpec(this);
		}
	}

}
