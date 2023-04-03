/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attributetype;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBAttributeType.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
class DBAttributeType extends DBAttributeTypeBase
{
	final String name;
	final String syntaxId;

	private DBAttributeType(Builder builder)
	{
		super(builder);
		this.name = builder.name;
		this.syntaxId = builder.syntaxId;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(name, syntaxId);
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
		DBAttributeType other = (DBAttributeType) obj;
		return Objects.equals(name, other.name) && Objects.equals(syntaxId, other.syntaxId);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder extends DBAttributeTypeBaseBuilder<Builder>
	{

		private String name;
		private String syntaxId;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withSyntaxId(String syntaxId)
		{
			this.syntaxId = syntaxId;
			return this;
		}

		public DBAttributeType build()
		{
			return new DBAttributeType(this);
		}
	}

}
