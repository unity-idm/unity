/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBGroup.Builder.class)
public class DBGroup
{
	public final DBI18nString displayedName;
	public final DBI18nString i18nDescription;
	public final String description;
	public final DBAttributeStatement[] attributeStatements;
	public final Set<String> attributesClasses;
	public final DBGroupDelegationConfiguration delegationConfiguration;
	public final boolean publicGroup;
	public final List<DBGroupProperty> properties;

	private DBGroup(Builder builder)
	{
		this.displayedName = builder.displayedName;
		this.i18nDescription = builder.i18nDescription;
		this.description = builder.description;
		this.attributeStatements = builder.attributeStatements;
		this.attributesClasses = Optional.ofNullable(builder.attributesClasses)
				.map(Set::copyOf)
				.orElse(null);
		this.delegationConfiguration = builder.delegationConfiguration;
		this.publicGroup = builder.publicGroup;
		this.properties = Optional.ofNullable(builder.properties)
				.map(List::copyOf)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(attributeStatements);
		result = prime * result + Objects.hash(attributesClasses, delegationConfiguration, displayedName,
				i18nDescription, properties, publicGroup, description);
		return result;
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
		DBGroup other = (DBGroup) obj;
		return Arrays.equals(attributeStatements, other.attributeStatements)
				&& Objects.equals(attributesClasses, other.attributesClasses)
				&& Objects.equals(delegationConfiguration, other.delegationConfiguration)
				&& Objects.equals(displayedName, other.displayedName)
				&& Objects.equals(i18nDescription, other.i18nDescription)
				&& Objects.equals(description, other.description) && Objects.equals(properties, other.properties)
				&& publicGroup == other.publicGroup;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private DBI18nString displayedName;
		private DBI18nString i18nDescription;
		private String description;
		private DBAttributeStatement[] attributeStatements = new DBAttributeStatement[0];
		private Set<String> attributesClasses = Collections.emptySet();
		private DBGroupDelegationConfiguration delegationConfiguration;
		private boolean publicGroup = false;
		private List<DBGroupProperty> properties = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withDisplayedName(DBI18nString displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public Builder withI18nDescription(DBI18nString i18nDescription)
		{
			this.i18nDescription = i18nDescription;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withAttributeStatements(DBAttributeStatement[] attributeStatements)
		{
			this.attributeStatements = attributeStatements;
			return this;
		}

		public Builder withAttributesClasses(Set<String> attributesClasses)
		{
			this.attributesClasses = Optional.ofNullable(attributesClasses)
					.map(Set::copyOf)
					.orElse(null);
			return this;
		}

		public Builder withDelegationConfiguration(DBGroupDelegationConfiguration delegationConfiguration)
		{
			this.delegationConfiguration = delegationConfiguration;
			return this;
		}

		public Builder withPublicGroup(boolean publicGroup)
		{
			this.publicGroup = publicGroup;
			return this;
		}

		public Builder withProperties(List<DBGroupProperty> properties)
		{
			this.properties = Optional.ofNullable(properties)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public DBGroup build()
		{
			return new DBGroup(this);
		}
	}
}
