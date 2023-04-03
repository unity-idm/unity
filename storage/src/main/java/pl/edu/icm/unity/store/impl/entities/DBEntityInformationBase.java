/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.entities;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBEntityInformationBase.DBEntityInformationBaseBuilder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class DBEntityInformationBase
{
	public final String state;
	@JsonProperty("ScheduledOperationTime")
	public final Date scheduledOperationTime;
	@JsonProperty("ScheduledOperation")
	public final String scheduledOperation;
	@JsonProperty("RemovalByUserTime")
	public final Date removalByUserTime;

	protected DBEntityInformationBase(DBEntityInformationBaseBuilder<?> builder)
	{
		this.state = builder.state;
		this.scheduledOperationTime = builder.scheduledOperationTime;
		this.scheduledOperation = builder.scheduledOperation;
		this.removalByUserTime = builder.removalByUserTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(removalByUserTime, scheduledOperation, scheduledOperationTime, state);
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
		DBEntityInformationBase other = (DBEntityInformationBase) obj;
		return Objects.equals(removalByUserTime, other.removalByUserTime)
				&& Objects.equals(scheduledOperation, other.scheduledOperation)
				&& Objects.equals(scheduledOperationTime, other.scheduledOperationTime)
				&& Objects.equals(state, other.state);
	}

	public static DBEntityInformationBaseBuilder<?> builder()
	{
		return new DBEntityInformationBaseBuilder<>();
	}

	public static class DBEntityInformationBaseBuilder<T extends DBEntityInformationBaseBuilder<?>>
	{

		private String state;
		@JsonProperty("ScheduledOperationTime")
		private Date scheduledOperationTime;
		@JsonProperty("ScheduledOperation")
		private String scheduledOperation;
		@JsonProperty("RemovalByUserTime")
		private Date removalByUserTime;

		protected DBEntityInformationBaseBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public T withState(String entityState)
		{
			this.state = entityState;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withScheduledOperationTime(Date scheduledOperationTime)
		{
			this.scheduledOperationTime = scheduledOperationTime;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withScheduledOperation(String scheduledOperation)
		{
			this.scheduledOperation = scheduledOperation;
			return (T) this;
		}

		@SuppressWarnings("unchecked")
		public T withRemovalByUserTime(Date removalByUserTime)
		{
			this.removalByUserTime = removalByUserTime;
			return (T) this;
		}

		public DBEntityInformationBase build()
		{
			return new DBEntityInformationBase(this);
		}
	}

}
