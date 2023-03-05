/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.entities;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBEntityInformation.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class DBEntityInformation extends DBEntityInformationBase
{
	public final Long entityId;

	private DBEntityInformation(Builder builder)
	{
		super(builder);
		this.entityId = builder.entityId;

	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(entityId);
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
		DBEntityInformation other = (DBEntityInformation) obj;
		return Objects.equals(entityId, other.entityId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends DBEntityInformationBaseBuilder<Builder>
	{
		private Long entityId;

		private Builder()
		{
		}

		public Builder withEntityId(Long id)
		{
			this.entityId = id;
			return this;
		}

		public DBEntityInformation build()
		{
			return new DBEntityInformation(this);
		}
	}

}
