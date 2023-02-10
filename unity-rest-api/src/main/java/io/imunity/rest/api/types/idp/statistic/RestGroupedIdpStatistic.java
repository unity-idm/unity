/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.idp.statistic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroupedIdpStatistic.Builder.class)
public class RestGroupedIdpStatistic
{
	public final String idpId;
	public final String idpName;
	public final String clientId;
	public final String clientName;
	public final List<RestSigInStatistic> sigInStats;

	private RestGroupedIdpStatistic(Builder builder)
	{
		this.idpId = builder.idpId;
		this.idpName = builder.idpName;
		this.clientId = builder.clientId;
		this.clientName = builder.clientName;
		this.sigInStats = Optional.ofNullable(builder.sigInStats)
				.map(List::copyOf)
				.orElse(null);
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
		RestGroupedIdpStatistic other = (RestGroupedIdpStatistic) obj;
		return Objects.equals(clientId, other.clientId) && Objects.equals(clientName, other.clientName)
				&& Objects.equals(idpId, other.idpId) && Objects.equals(idpName, other.idpName)
				&& Objects.equals(sigInStats, other.sigInStats);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String idpId;
		private String idpName;
		private String clientId;
		private String clientName;
		private List<RestSigInStatistic> sigInStats = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withIdpId(String idpId)
		{
			this.idpId = idpId;
			return this;
		}

		public Builder withIdpName(String idpName)
		{
			this.idpName = idpName;
			return this;
		}

		public Builder withClientId(String clientId)
		{
			this.clientId = clientId;
			return this;
		}

		public Builder withClientName(String clientName)
		{
			this.clientName = clientName;
			return this;
		}

		public Builder withSigInStats(List<RestSigInStatistic> sigInStats)
		{
			this.sigInStats = Optional.ofNullable(sigInStats)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public RestGroupedIdpStatistic build()
		{
			return new RestGroupedIdpStatistic(this);
		}
	}

}