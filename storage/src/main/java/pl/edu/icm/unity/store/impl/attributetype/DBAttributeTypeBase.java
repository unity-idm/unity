/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attributetype;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.types.DBI18nString;

@JsonDeserialize(builder = DBAttributeTypeBase.DBAttributeTypeBaseBuilder.class)
class DBAttributeTypeBase
{
	public final DBI18nString displayedName;
	public final DBI18nString i18nDescription;
	public final String description;
	public final JsonNode syntaxState;
	public final int minElements;
	public final int maxElements;
	public final boolean uniqueValues;
	public final boolean selfModificable;
	public final boolean global;
	public final int flags;
	public final Map<String, String> metadata;

	protected DBAttributeTypeBase(DBAttributeTypeBaseBuilder<?> builder)
	{
		this.displayedName = builder.displayedName;
		this.i18nDescription = builder.i18nDescription;
		this.description = builder.description;
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
		return Objects.hash(displayedName, flags, global, i18nDescription, description, maxElements, metadata,
				minElements, selfModificable, syntaxState, uniqueValues);
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
		DBAttributeTypeBase other = (DBAttributeTypeBase) obj;
		return Objects.equals(displayedName, other.displayedName) && flags == other.flags && global == other.global
				&& Objects.equals(i18nDescription, other.i18nDescription)
				&& Objects.equals(description, other.description) && maxElements == other.maxElements
				&& Objects.equals(metadata, other.metadata) && minElements == other.minElements
				&& selfModificable == other.selfModificable && Objects.equals(syntaxState, other.syntaxState)
				&& uniqueValues == other.uniqueValues;
	}

	public static DBAttributeTypeBaseBuilder<?> builder()
	{
		return new DBAttributeTypeBaseBuilder<>();
	}

	public static class DBAttributeTypeBaseBuilder<T extends DBAttributeTypeBaseBuilder<?>>
	{
		private DBI18nString displayedName;
		private DBI18nString i18nDescription;
		private String description;
		private JsonNode syntaxState;
		private int minElements = 0;
		private int maxElements = 1;
		private boolean uniqueValues = false;
		private boolean selfModificable = false;
		private boolean global = false;
		private int flags = 0;
		private Map<String, String> metadata = Collections.emptyMap();

		protected DBAttributeTypeBaseBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withDisplayedName(DBI18nString displayedName)
		{
			this.displayedName = displayedName;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withI18nDescription(DBI18nString i18nDescription)
		{
			this.i18nDescription = i18nDescription;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withDescription(String description)
		{
			this.description = description;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withSyntaxState(JsonNode syntaxState)
		{
			this.syntaxState = syntaxState == null || syntaxState.isNull() ? null : syntaxState;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMinElements(int minElements)
		{
			this.minElements = minElements;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMaxElements(int maxElements)
		{
			this.maxElements = maxElements;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withUniqueValues(boolean uniqueValues)
		{
			this.uniqueValues = uniqueValues;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withSelfModificable(boolean selfModificable)
		{
			this.selfModificable = selfModificable;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withGlobal(boolean global)
		{
			this.global = global;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withFlags(int flags)
		{
			this.flags = flags;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withMetadata(Map<String, String> metadata)
		{
			this.metadata = Optional.ofNullable(metadata)
					.map(Map::copyOf)
					.orElse(null);
			return (T) this;
		}

		public DBAttributeTypeBase build()
		{
			return new DBAttributeTypeBase(this);
		}
	}

}
