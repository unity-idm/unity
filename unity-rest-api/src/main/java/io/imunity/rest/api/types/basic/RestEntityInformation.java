/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEntityInformation.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestEntityInformation
{
	public final Long id;
	public final String entityState;
	public final Date scheduledOperationTime;
	public final String scheduledOperation;
	public final Date removalByUserTime;

	private RestEntityInformation(Builder builder)
	{
		this.id = builder.id;
		this.entityState = builder.entityState;
		this.scheduledOperationTime = builder.scheduledOperationTime;
		this.scheduledOperation = builder.scheduledOperation;
		this.removalByUserTime = builder.removalByUserTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityState, id, removalByUserTime, scheduledOperation, scheduledOperationTime);
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
		return Objects.equals(entityState, other.entityState) && Objects.equals(id, other.id)
				&& Objects.equals(removalByUserTime, other.removalByUserTime)
				&& Objects.equals(scheduledOperation, other.scheduledOperation)
				&& Objects.equals(scheduledOperationTime, other.scheduledOperationTime);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Long id;
		private String entityState;
		private Date scheduledOperationTime;
		private String scheduledOperation;
		private Date removalByUserTime;

		private Builder()
		{
		}

		public Builder withId(Long id)
		{
			this.id = id;
			return this;
		}

		public Builder withEntityState(String entityState)
		{
			this.entityState = entityState;
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
