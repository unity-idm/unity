/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identities;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBIdentity.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DBIdentity extends DBIdentityBase
{
	public final long entityId;
	public final String typeId;

	private DBIdentity(Builder builder)
	{
		super(builder);
		this.entityId = builder.entityId;
		this.typeId = builder.typeId;

	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(entityId, typeId);
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
		DBIdentity other = (DBIdentity) obj;
		return entityId == other.entityId && Objects.equals(typeId, other.typeId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends DBIdentityBaseBuilder<Builder>
	{
		private long entityId;

		private String typeId;

		private Builder()
		{
		}

		public Builder withEntityId(long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public Builder withTypeId(String typeId)
		{
			this.typeId = typeId;
			return this;
		}

		public DBIdentity build()
		{
			return new DBIdentity(this);
		}
	}

}
