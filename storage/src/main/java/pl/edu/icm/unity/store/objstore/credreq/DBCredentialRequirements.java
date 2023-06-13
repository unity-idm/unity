/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.credreq;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBCredentialRequirements.Builder.class)
class DBCredentialRequirements
{
	final String name;
	final String description;
	final Set<String> requiredCredentials;
	final boolean readOnly;

	private DBCredentialRequirements(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.requiredCredentials = builder.requiredCredentials;
		this.readOnly = builder.readOnly;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, name, readOnly, requiredCredentials);
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
		DBCredentialRequirements other = (DBCredentialRequirements) obj;
		return Objects.equals(description, other.description) && Objects.equals(name, other.name)
				&& readOnly == other.readOnly && Objects.equals(requiredCredentials, other.requiredCredentials);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private Set<String> requiredCredentials = Collections.emptySet();
		private boolean readOnly;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withRequiredCredentials(Set<String> requiredCredentials)
		{
			this.requiredCredentials = requiredCredentials;
			return this;
		}

		public Builder withReadOnly(boolean readOnly)
		{
			this.readOnly = readOnly;
			return this;
		}

		public DBCredentialRequirements build()
		{
			return new DBCredentialRequirements(this);
		}
	}
}
