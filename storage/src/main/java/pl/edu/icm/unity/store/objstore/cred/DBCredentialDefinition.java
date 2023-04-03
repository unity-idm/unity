/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.cred;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.common.DBI18nString;

@JsonDeserialize(builder = DBCredentialDefinition.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
class DBCredentialDefinition
{
	public final String name;
	public final String typeId;
	public final String configuration;
	public final boolean readOnly;
	public final DBI18nString displayedName;
	public final DBI18nString i18nDescription;

	private DBCredentialDefinition(Builder builder)
	{
		this.name = builder.name;
		this.typeId = builder.typeId;
		this.configuration = builder.configuration;
		this.readOnly = builder.readOnly;
		this.displayedName = builder.displayedName;
		this.i18nDescription = builder.description;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(configuration, i18nDescription, displayedName, name, readOnly, typeId);
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
		DBCredentialDefinition other = (DBCredentialDefinition) obj;
		return Objects.equals(configuration, other.configuration) && Objects.equals(i18nDescription, other.i18nDescription)
				&& Objects.equals(displayedName, other.displayedName) && Objects.equals(name, other.name)
				&& readOnly == other.readOnly && Objects.equals(typeId, other.typeId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder
	{
		private String name;
		private String typeId;
		private String configuration;
		private boolean readOnly;
		private DBI18nString displayedName;
		private DBI18nString description;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withTypeId(String typeId)
		{
			this.typeId = typeId;
			return this;
		}

		public Builder withConfiguration(String configuration)
		{
			this.configuration = configuration;
			return this;
		}

		public Builder withReadOnly(boolean readOnly)
		{
			this.readOnly = readOnly;
			return this;
		}

		public Builder withDisplayedName(DBI18nString displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public Builder withI18nDescription(DBI18nString description)
		{
			this.description = description;
			return this;
		}

		public DBCredentialDefinition build()
		{
			return new DBCredentialDefinition(this);
		}
	}

}
