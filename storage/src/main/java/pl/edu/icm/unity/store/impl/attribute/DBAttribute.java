/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAttribute.DBAttributeBuilder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBAttribute extends DBAttributeBase
{
	public final String name;
	public final String valueSyntax;
	public final String groupPath;

	protected DBAttribute(DBAttributeBuilder<?> builder)
	{
		super(builder);
		this.name = builder.name;
		this.valueSyntax = builder.valueSyntax;
		this.groupPath = builder.groupPath;

	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(groupPath, name, valueSyntax);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBAttribute other = (DBAttribute) obj;
		return Objects.equals(groupPath, other.groupPath) && Objects.equals(name, other.name)
				&& Objects.equals(valueSyntax, other.valueSyntax);
	}

	public static DBAttributeBuilder<?> builder()
	{
		return new DBAttributeBuilder<>();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DBAttributeBuilder<S extends DBAttributeBuilder<S>>
			extends DBAttributeBaseBuilder<S>
	{
		private String name;
		private String valueSyntax;
		private String groupPath;

		protected DBAttributeBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public S withName(String name)
		{
			this.name = name;
			return (S) this;
		}

		@SuppressWarnings("unchecked")
		public S withValueSyntax(String valueSyntax)
		{
			this.valueSyntax = valueSyntax;
			return (S) this;
		}

		@SuppressWarnings("unchecked")
		public S withGroupPath(String groupPath)
		{
			this.groupPath = groupPath;
			return (S) this;
		}

		public DBAttribute build()
		{
			return new DBAttribute(this);
		}
	}

}