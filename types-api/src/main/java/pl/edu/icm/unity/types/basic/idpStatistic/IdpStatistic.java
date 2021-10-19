/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic.idpStatistic;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IdpStatistic
{
	public static enum Status
	{
		SUCCESSFUL, FAILED
	};

	public final LocalDateTime timestamp;
	public final String idpEndpointId;
	public final String idpEndpointName;
	public final String clientId;
	public final String clientName;
	public final Status status;

	private IdpStatistic(Builder builder)
	{
		this.timestamp = builder.timestamp;
		this.idpEndpointId = builder.idpEndpointId;
		this.idpEndpointName = builder.idpEndpointName;
		this.clientId = builder.clientId;
		this.clientName = builder.clientName;
		this.status = builder.status;
	}

	@JsonCreator
	public IdpStatistic(@JsonProperty("timestamp") LocalDateTime timestamp,
			@JsonProperty("idpEndpointId") String idpEndpointId,
			@JsonProperty("idpEndpointName") String idpEndpointName, @JsonProperty("clientId") String clientId,
			@JsonProperty("clientName") String clientName, @JsonProperty("status") Status status)
	{
		this.timestamp = timestamp;
		this.idpEndpointId = idpEndpointId;
		this.idpEndpointName = idpEndpointName;
		this.clientId = clientId;
		this.clientName = clientName;
		this.status = status;

		requireNonNull(timestamp, "IdpStatistic.timestamp field is required!");
		requireNonNull(clientId, "IdpStatistic.clientId field is required!");
		requireNonNull(idpEndpointId, "IdpStatistic.idpEndpointId field is required!");
		requireNonNull(status, "IdpStatistic.status field is required!");
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(clientName, clientId, idpEndpointId, idpEndpointName, status, timestamp);
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
		IdpStatistic other = (IdpStatistic) obj;
		return Objects.equals(clientName, other.clientName) && Objects.equals(clientId, other.clientId)
				&& Objects.equals(idpEndpointId, other.idpEndpointId)
				&& Objects.equals(idpEndpointName, other.idpEndpointName) && status == other.status
				&& Objects.equals(timestamp, other.timestamp);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private LocalDateTime timestamp;
		private String idpEndpointId;
		private String idpEndpointName;
		private String clientId;
		private String clientName;
		private Status status;

		private Builder()
		{
		}

		public Builder timestamp(LocalDateTime timestamp)
		{
			this.timestamp = timestamp;
			return this;
		}

		public Builder idpEndpointId(String idpEndpointId)
		{
			this.idpEndpointId = idpEndpointId;
			return this;
		}

		public Builder idpEndpointName(String idpEndpointName)
		{
			this.idpEndpointName = idpEndpointName;
			return this;
		}

		public Builder clientId(String clientId)
		{
			this.clientId = clientId;
			return this;
		}

		public Builder clientName(String clientName)
		{
			this.clientName = clientName;
			return this;
		}

		public Builder status(Status status)
		{
			this.status = status;
			return this;
		}

		public IdpStatistic build()
		{
			return new IdpStatistic(this);
		}
	}
}
