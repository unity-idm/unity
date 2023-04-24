/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.common.DBI18nString;

@JsonDeserialize(builder = DBGroupBase.DBGroupBaseBuilder.class)
class DBGroupBase
{
	final DBI18nString displayedName;
	final DBI18nString i18nDescription;
	final String description;
	final DBAttributeStatement[] attributeStatements;
	final Set<String> attributesClasses;
	final DBGroupDelegationConfiguration delegationConfiguration;
	final boolean publicGroup;
	final List<DBGroupProperty> properties;

	protected DBGroupBase(DBGroupBaseBuilder<?> builder)
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
		DBGroupBase other = (DBGroupBase) obj;
		return Arrays.equals(attributeStatements, other.attributeStatements)
				&& Objects.equals(attributesClasses, other.attributesClasses)
				&& Objects.equals(delegationConfiguration, other.delegationConfiguration)
				&& Objects.equals(displayedName, other.displayedName)
				&& Objects.equals(i18nDescription, other.i18nDescription)
				&& Objects.equals(description, other.description) && Objects.equals(properties, other.properties)
				&& publicGroup == other.publicGroup;
	}

	static DBGroupBaseBuilder<?> builder()
	{
		return new DBGroupBaseBuilder<>();
	}

	static class DBGroupBaseBuilder<T extends DBGroupBaseBuilder<?>>
	{
		private DBI18nString displayedName;
		private DBI18nString i18nDescription;
		private String description;
		private DBAttributeStatement[] attributeStatements = new DBAttributeStatement[0];
		private Set<String> attributesClasses = Collections.emptySet();
		private DBGroupDelegationConfiguration delegationConfiguration;
		private boolean publicGroup = false;
		private List<DBGroupProperty> properties = Collections.emptyList();

		protected DBGroupBaseBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		T withDisplayedName(DBI18nString displayedName)
		{
			this.displayedName = displayedName;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		T withI18nDescription(DBI18nString i18nDescription)
		{
			this.i18nDescription = i18nDescription;
			return (T) this;
		}
		@SuppressWarnings("unchecked")
		T withDescription(String description)
		{
			this.description = description;
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		T withAttributeStatements(DBAttributeStatement[] attributeStatements)
		{
			this.attributeStatements = attributeStatements;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		T withAttributesClasses(Set<String> attributesClasses)
		{
			this.attributesClasses = Optional.ofNullable(attributesClasses)
					.map(Set::copyOf)
					.orElse(null);
			return (T) this;
		}
		
		@SuppressWarnings("unchecked")
		T withDelegationConfiguration(DBGroupDelegationConfiguration delegationConfiguration)
		{
			this.delegationConfiguration = delegationConfiguration;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		T withPublicGroup(boolean publicGroup)
		{
			this.publicGroup = publicGroup;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		T withProperties(List<DBGroupProperty> properties)
		{
			this.properties = Optional.ofNullable(properties)
					.map(List::copyOf)
					.orElse(null);
			return (T) this;
		}

		DBGroupBase build()
		{
			return new DBGroupBase(this);
		}
	}
}
