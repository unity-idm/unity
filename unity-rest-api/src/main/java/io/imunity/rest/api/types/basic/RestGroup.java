/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroup.Builder.class)
public class RestGroup
{
	public final String path;
	public final RestI18nString displayedName;
	public final RestI18nString i18nDescription;

	public final RestAttributeStatement[] attributeStatements;
	public final Set<String> attributesClasses;
	public final RestGroupDelegationConfiguration delegationConfiguration;
	public final boolean publicGroup;
	public final List<RestGroupProperty> properties;

	private RestGroup(Builder builder)
	{
		this.path = builder.path;
		this.displayedName = builder.displayedName;
		this.i18nDescription = builder.i18nDescription;
		this.attributeStatements = builder.attributeStatements;
		this.attributesClasses = builder.attributesClasses;
		this.delegationConfiguration = builder.delegationConfiguration;
		this.publicGroup = builder.publicGroup;
		this.properties = builder.properties;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(attributeStatements);
		result = prime * result + Objects.hash(attributesClasses, delegationConfiguration, displayedName,
				i18nDescription, path, properties, publicGroup);
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
		RestGroup other = (RestGroup) obj;
		return Arrays.equals(attributeStatements, other.attributeStatements)
				&& Objects.equals(attributesClasses, other.attributesClasses)
				&& Objects.equals(delegationConfiguration, other.delegationConfiguration)
				&& Objects.equals(displayedName, other.displayedName)
				&& Objects.equals(i18nDescription, other.i18nDescription) && Objects.equals(path, other.path)
				&& Objects.equals(properties, other.properties) && publicGroup == other.publicGroup;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String path;
		private RestI18nString displayedName;
		private RestI18nString i18nDescription;
		private RestAttributeStatement[] attributeStatements = new RestAttributeStatement[0];
		private Set<String> attributesClasses = Collections.emptySet();
		private RestGroupDelegationConfiguration delegationConfiguration;
		private boolean publicGroup = false;
		private List<RestGroupProperty> properties = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withPath(String path)
		{
			this.path = path;
			return this;
		}

		public Builder withDisplayedName(RestI18nString displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public Builder withI18nDescription(RestI18nString i18nDescription)
		{
			this.i18nDescription = i18nDescription;
			return this;
		}

		public Builder withAttributeStatements(RestAttributeStatement[] attributeStatements)
		{
			this.attributeStatements = attributeStatements;
			return this;
		}

		public Builder withAttributesClasses(Set<String> attributesClasses)
		{
			this.attributesClasses = attributesClasses;
			return this;
		}

		public Builder withDelegationConfiguration(RestGroupDelegationConfiguration delegationConfiguration)
		{
			this.delegationConfiguration = delegationConfiguration;
			return this;
		}

		public Builder withPublicGroup(boolean publicGroup)
		{
			this.publicGroup = publicGroup;
			return this;
		}

		public Builder withProperties(List<RestGroupProperty> properties)
		{
			this.properties = properties;
			return this;
		}

		public RestGroup build()
		{
			if(Objects.isNull(displayedName))
			{
				throw new IllegalArgumentException("Displayed name cannot be null");
			}
			
			return new RestGroup(this);
		}
	}

	
}
