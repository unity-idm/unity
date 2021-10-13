/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic.idpStatistic;

import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.Objects;

public class IdpStatistic
{
	public static enum Status { SUCCESSFUL, FAILED};
	
	private Date timestamp;
	private String idpEndpointId;
	private String idpEndpointName;
	private String clientId;
	private String clientName;
	private Status status;
	
	public IdpStatistic()
	{
		
	}
	
	public Date getTimestamp()
	{
		return timestamp;
	}
	public String getIdpEndpointId()
	{
		return idpEndpointId;
	}
	public String getIdpEndpointName()
	{
		return idpEndpointName;
	}
	public String getClientId()
	{
		return clientId;
	}
	public String getClientName()
	{
		return clientName;
	}
	public Status getStatus()
	{
		return status;
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
		return Objects.equals(clientName, other.clientName)
				&& Objects.equals(clientId, other.clientId) && Objects.equals(idpEndpointId, other.idpEndpointId)
				&& Objects.equals(idpEndpointName, other.idpEndpointName) && status == other.status
				&& Objects.equals(timestamp, other.timestamp);
	}
	
	public static IdpStatisticBuilder builder()
	{
		return new IdpStatisticBuilder();
	}

	public static class IdpStatisticBuilder
	{
		private IdpStatistic statistic = new IdpStatistic();

		public IdpStatisticBuilder timestamp(final Date timestamp)
		{
			statistic.timestamp = timestamp;
			return this;
		}

		public IdpStatisticBuilder idpEndpointId(final String idpEndpointId)
		{
			statistic.idpEndpointId = idpEndpointId;
			return this;
		}
		
		public IdpStatisticBuilder idpEndpointName(final String idpEndpointName)
		{
			statistic.idpEndpointName = idpEndpointName;
			return this;
		}

		public IdpStatisticBuilder clientId(final String clientId)
		{
			statistic.clientId = clientId;
			return this;
		}
		
		public IdpStatisticBuilder clientName(final String clientDisplayedName)
		{
			statistic.clientName = clientDisplayedName;
			return this;
		}
		
		public IdpStatisticBuilder status(final Status status)
		{
			statistic.status = status;
			return this;
		}
		
		public IdpStatistic build()
		{	
			requireNonNull(statistic.timestamp, "IdpStatistic.timestamp field is required!");
			requireNonNull(statistic.clientId, "IdpStatistic.clientId field is required!");
			requireNonNull(statistic.idpEndpointId, "IdpStatistic.idpEndpointId field is required!");
			requireNonNull(statistic.status, "IdpStatistic.status field is required!");
			return statistic;
		}
	}
}
