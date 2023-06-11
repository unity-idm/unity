/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp.statistic;

import java.util.Objects;

import pl.edu.icm.unity.base.idpStatistic.IdpStatistic.Status;

public class IdpStatisticEvent
{
	public final String idpEndpointId;
	public final String idpEndpointName;
	public final String clientId;
	public final String clientName;
	public final Status status;

	public IdpStatisticEvent(String idpEndpointId, String idpEndpointName, String clientId, String clientDisplayedName,
			Status status)
	{
		this.idpEndpointId = idpEndpointId;
		this.idpEndpointName = idpEndpointName;
		this.clientId = clientId;
		this.clientName = clientDisplayedName;
		this.status = status;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(clientName, clientId, idpEndpointId, idpEndpointName, status);
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
		IdpStatisticEvent other = (IdpStatisticEvent) obj;
		return Objects.equals(clientName, other.clientName)
				&& Objects.equals(clientId, other.clientId) && Objects.equals(idpEndpointId, other.idpEndpointId)
				&& Objects.equals(idpEndpointName, other.idpEndpointName) && status == other.status;
	}

}
