/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.common.DBI18nString;

@JsonDeserialize(builder = DBEndpointConfiguration.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
class DBEndpointConfiguration
{
	public final DBI18nString displayedName;
	public final String description;
	public final List<String> authenticationOptions;
	public final String configuration;
	public final String realm;
	public final String tag;

	private DBEndpointConfiguration(Builder builder)
	{
		this.displayedName = builder.displayedName;
		this.description = builder.description;
		this.authenticationOptions = Optional.ofNullable(builder.authenticationOptions)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.configuration = builder.configuration;
		this.realm = builder.realm;
		this.tag = builder.tag;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authenticationOptions, configuration, description, displayedName, realm, tag);
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
		DBEndpointConfiguration other = (DBEndpointConfiguration) obj;
		return Objects.equals(authenticationOptions, other.authenticationOptions)
				&& Objects.equals(configuration, other.configuration) && Objects.equals(description, other.description)
				&& Objects.equals(displayedName, other.displayedName) && Objects.equals(realm, other.realm)
				&& Objects.equals(tag, other.tag);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private DBI18nString displayedName;
		private String description;
		private List<String> authenticationOptions;
		private String configuration;
		private String realm;
		private String tag;

		private Builder()
		{
		}

		public Builder withDisplayedName(DBI18nString displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withAuthenticationOptions(List<String> authenticationOptions)
		{
			this.authenticationOptions = Optional.ofNullable(authenticationOptions)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return this;
		}

		public Builder withConfiguration(String configuration)
		{
			this.configuration = configuration;
			return this;
		}

		public Builder withRealm(String realm)
		{
			this.realm = realm;
			return this;
		}

		public Builder withTag(String tag)
		{
			this.tag = tag;
			return this;
		}

		public DBEndpointConfiguration build()
		{
			return new DBEndpointConfiguration(this);
		}
	}

}
