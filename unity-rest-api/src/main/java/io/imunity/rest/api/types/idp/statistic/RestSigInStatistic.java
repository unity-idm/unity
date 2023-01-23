/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.idp.statistic;

import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestSigInStatistic.Builder.class)
public class RestSigInStatistic
{
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	public final LocalDateTime periodStart;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
	public final LocalDateTime periodEnd;
	public final long totatCount;
	public final long successfullCount;
	public final long failedCount;

	private RestSigInStatistic(Builder builder)
	{
		this.periodStart = builder.periodStart;
		this.periodEnd = builder.periodEnd;
		this.totatCount = builder.totatCount;
		this.successfullCount = builder.successfullCount;
		this.failedCount = builder.failedCount;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(failedCount, periodEnd, periodStart, successfullCount, totatCount);
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
		RestSigInStatistic other = (RestSigInStatistic) obj;
		return failedCount == other.failedCount && Objects.equals(periodEnd, other.periodEnd)
				&& Objects.equals(periodStart, other.periodStart) && successfullCount == other.successfullCount
				&& totatCount == other.totatCount;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
		private LocalDateTime periodStart;
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
		private LocalDateTime periodEnd;
		private long totatCount;
		private long successfullCount;
		private long failedCount;

		private Builder()
		{
		}

		public Builder withPeriodStart(LocalDateTime periodStart)
		{
			this.periodStart = periodStart;
			return this;
		}

		public Builder withPeriodEnd(LocalDateTime periodEnd)
		{
			this.periodEnd = periodEnd;
			return this;
		}

		public Builder withTotatCount(long totatCount)
		{
			this.totatCount = totatCount;
			return this;
		}

		public Builder withSuccessfullCount(long successfullCount)
		{
			this.successfullCount = successfullCount;
			return this;
		}

		public Builder withFailedCount(long failedCount)
		{
			this.failedCount = failedCount;
			return this;
		}

		public RestSigInStatistic build()
		{
			return new RestSigInStatistic(this);
		}
	}

}