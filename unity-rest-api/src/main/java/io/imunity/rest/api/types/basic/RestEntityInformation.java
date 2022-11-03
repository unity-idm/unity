/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEntityInformation.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestEntityInformation
{
	public final Long entityId;
	public final String state;
	@JsonProperty("ScheduledOperationTime")
	public final Date scheduledOperationTime;
	@JsonProperty("ScheduledOperation")
	public final String scheduledOperation;
	@JsonProperty("RemovalByUserTime")
	public final Date removalByUserTime;

	private RestEntityInformation(Builder builder)
	{
		this.entityId = builder.entityId;
		this.state = builder.state;
		this.scheduledOperationTime = builder.scheduledOperationTime;
		this.scheduledOperation = builder.scheduledOperation;
		this.removalByUserTime = builder.removalByUserTime;
	}

	
	
	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, removalByUserTime, scheduledOperation, scheduledOperationTime, state);
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
		RestEntityInformation other = (RestEntityInformation) obj;
		return Objects.equals(entityId, other.entityId) && Objects.equals(removalByUserTime, other.removalByUserTime)
				&& Objects.equals(scheduledOperation, other.scheduledOperation)
				&& Objects.equals(scheduledOperationTime, other.scheduledOperationTime)
				&& Objects.equals(state, other.state);
	}



	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Long entityId;
		private String state;
		@JsonProperty("ScheduledOperationTime")
		private Date scheduledOperationTime;
		@JsonProperty("ScheduledOperation")
		private String scheduledOperation;
		@JsonProperty("RemovalByUserTime")
		private Date removalByUserTime;

		private Builder()
		{
		}

		public Builder withEntityId(Long id)
		{
			this.entityId = id;
			return this;
		}

		public Builder withState(String entityState)
		{
			this.state = entityState;
			return this;
		}

		public Builder withScheduledOperationTime(Date scheduledOperationTime)
		{
			this.scheduledOperationTime = scheduledOperationTime;
			return this;
		}

		public Builder withScheduledOperation(String scheduledOperation)
		{
			this.scheduledOperation = scheduledOperation;
			return this;
		}

		public Builder withRemovalByUserTime(Date removalByUserTime)
		{
			this.removalByUserTime = removalByUserTime;
			return this;
		}

		public RestEntityInformation build()
		{
			return new RestEntityInformation(this);
		}
	}

}
