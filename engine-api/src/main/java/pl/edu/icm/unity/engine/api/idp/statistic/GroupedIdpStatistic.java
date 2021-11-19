/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp.statistic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class GroupedIdpStatistic
{
	public final String idpId;
	public final String idpName;
	public final String clientId;
	public final String clientName;
	public final List<SigInStatistic> sigInStats;

	@JsonCreator
	public GroupedIdpStatistic(@JsonProperty("idpId") String idpId, @JsonProperty("idpName") String idpName,
			@JsonProperty("clientId") String clientId, @JsonProperty("clientName") String clientName,
			@JsonProperty("sigInStats") List<SigInStatistic> sigInStats)
	{
		this.idpId = idpId;
		this.idpName = idpName;
		this.clientId = clientId;
		this.clientName = clientName;
		this.sigInStats = ImmutableList.copyOf(sigInStats);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(clientId, clientName, idpId, idpName, sigInStats);
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
		GroupedIdpStatistic other = (GroupedIdpStatistic) obj;
		return Objects.equals(clientId, other.clientId) && Objects.equals(clientName, other.clientName)
				&& Objects.equals(idpId, other.idpId) && Objects.equals(idpName, other.idpName)
				&& Objects.equals(sigInStats, other.sigInStats);
	}

	public static class SigInStatistic
	{
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
		public final LocalDateTime periodStart;
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
		public final LocalDateTime periodEnd;
		public final long totatCount;
		public final long successfullCount;
		public final long failedCount;

		@JsonCreator
		public SigInStatistic(@JsonProperty("periodStart") LocalDateTime periodStart, @JsonProperty("periodEnd") LocalDateTime periodEnd,
				@JsonProperty("totatCount") long totatCount, @JsonProperty("successfullCount") long successfullCount,
				@JsonProperty("failedCount") long failedCount)
		{
			this.periodStart = periodStart;
			this.periodEnd = periodEnd;
			this.totatCount = totatCount;
			this.successfullCount = successfullCount;
			this.failedCount = failedCount;
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
			SigInStatistic other = (SigInStatistic) obj;
			return failedCount == other.failedCount && Objects.equals(periodEnd, other.periodEnd)
					&& Objects.equals(periodStart, other.periodStart) && successfullCount == other.successfullCount
					&& totatCount == other.totatCount;
		}

	}

}