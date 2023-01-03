/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestAttributeType.Builder.class)
public class RestAttributeType
{
	public final String name;
	public final RestI18nString displayedName;
	public final RestI18nString i18nDescription;
	public final String syntaxId;
	public final JsonNode syntaxState;
	public final int minElements;
	public final int maxElements;
	public final boolean uniqueValues;
	public final boolean selfModificable;
	public final boolean global;
	public final int flags;
	public final Map<String, String> metadata;

	private RestAttributeType(Builder builder)
	{
		this.name = builder.name;
		this.displayedName = builder.displayedName;
		this.i18nDescription = builder.i18nDescription;
		this.syntaxId = builder.syntaxId;
		this.syntaxState = builder.syntaxState;
		this.minElements = builder.minElements;
		this.maxElements = builder.maxElements;
		this.uniqueValues = builder.uniqueValues;
		this.selfModificable = builder.selfModificable;
		this.global = builder.global;
		this.flags = builder.flags;
		this.metadata = Optional.ofNullable(builder.metadata)
				.map(Map::copyOf)
				.orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(displayedName, flags, global, i18nDescription, maxElements, metadata, minElements, name,
				selfModificable, syntaxState, uniqueValues, syntaxId);
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
		RestAttributeType other = (RestAttributeType) obj;
		return Objects.equals(displayedName, other.displayedName) && flags == other.flags && global == other.global
				&& Objects.equals(i18nDescription, other.i18nDescription) && maxElements == other.maxElements
				&& Objects.equals(metadata, other.metadata) && minElements == other.minElements
				&& Objects.equals(name, other.name) && selfModificable == other.selfModificable
				&& Objects.equals(syntaxState, other.syntaxState) && uniqueValues == other.uniqueValues
				&& Objects.equals(syntaxId, other.syntaxId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private RestI18nString displayedName;
		private RestI18nString i18nDescription;
		private String syntaxId;
		private JsonNode syntaxState;
		private int minElements = 0;
		private int maxElements = 1;
		private boolean uniqueValues = false;
		private boolean selfModificable = false;
		private boolean global = false;
		private int flags = 0;
		private Map<String, String> metadata = Collections.emptyMap();

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
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

		public Builder withSyntaxId(String syntaxId)
		{
			this.syntaxId = syntaxId;
			return this;
		}

		public Builder withSyntaxState(JsonNode syntaxState)
		{
			this.syntaxState = syntaxState == null || syntaxState.isNull() ? null : syntaxState;
			return this;
		}

		public Builder withMinElements(int minElements)
		{
			this.minElements = minElements;
			return this;
		}

		public Builder withMaxElements(int maxElements)
		{
			this.maxElements = maxElements;
			return this;
		}

		public Builder withUniqueValues(boolean uniqueValues)
		{
			this.uniqueValues = uniqueValues;
			return this;
		}

		public Builder withSelfModificable(boolean selfModificable)
		{
			this.selfModificable = selfModificable;
			return this;
		}

		public Builder withGlobal(boolean global)
		{
			this.global = global;
			return this;
		}

		public Builder withFlags(int flags)
		{
			this.flags = flags;
			return this;
		}

		public Builder withMetadata(Map<String, String> metadata)
		{
			this.metadata = Optional.ofNullable(metadata)
					.map(Map::copyOf)
					.orElse(null);
			return this;
		}

		public RestAttributeType build()
		{
			if (displayedName == null || displayedName.defaultValue == null)			
			{
				displayedName = RestI18nString.builder()
						.withValues(Optional.ofNullable(displayedName).map(d -> d.values).orElse(Collections.emptyMap()))
						.withDefaultValue(name)
						.build();
			}

			return new RestAttributeType(this);
		}
	}

}
