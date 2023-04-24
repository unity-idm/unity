/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBGroupMembership.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class DBGroupMembership extends DBGroupMembershipBase
{
	final String group;
	final long entityId;

	private DBGroupMembership(Builder builder)
	{
		super(builder);
		this.group = builder.group;
		this.entityId = builder.entityId;

	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(entityId, group);
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
		DBGroupMembership other = (DBGroupMembership) obj;
		return entityId == other.entityId && Objects.equals(group, other.group);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends DBGroupMembershipBaseBuilder<Builder>
	{
		private String group;
		private long entityId;

		private Builder()
		{
		}

		public Builder withGroup(String group)
		{
			this.group = group;
			return this;
		}

		public Builder withEntityId(long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public DBGroupMembership build()
		{
			return new DBGroupMembership(this);
		}
	}

}
