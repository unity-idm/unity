/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.idpStatistics;

import java.time.LocalDateTime;
import java.util.Objects;

public class IdpStatisticBean
{
	private Long id;
	private LocalDateTime timestamp;
	private String idpEndpointId;
	private String idpEndpointName;
	private String clientId;
	private String clientName;
	private String status;

	public IdpStatisticBean(Long id, LocalDateTime timestamp, String idpEndpointId, String idpEndpointName,
			String clientId, String clientName, String status)
	{
		this.id = id;
		this.timestamp = timestamp;
		this.idpEndpointId = idpEndpointId;
		this.idpEndpointName = idpEndpointName;
		this.clientId = clientId;
		this.clientName = clientName;
		this.status = status;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(clientName, clientId, id, idpEndpointId, idpEndpointName, status, timestamp);
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
		IdpStatisticBean other = (IdpStatisticBean) obj;
		return Objects.equals(clientName, other.clientName) && Objects.equals(clientId, other.clientId)
				&& Objects.equals(id, other.id) && Objects.equals(idpEndpointId, other.idpEndpointId)
				&& Objects.equals(idpEndpointName, other.idpEndpointName) && Objects.equals(status, other.status)
				&& Objects.equals(timestamp, other.timestamp);
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public LocalDateTime getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getIdpEndpointId()
	{
		return idpEndpointId;
	}

	public void setIdpEndpointId(String idpEndpointId)
	{
		this.idpEndpointId = idpEndpointId;
	}

	public String getIdpEndpointName()
	{
		return idpEndpointName;
	}

	public void setIdpEndpointName(String idpEndpointName)
	{
		this.idpEndpointName = idpEndpointName;
	}

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	public String getClientName()
	{
		return clientName;
	}

	public void setClientName(String clientDisplayedName)
	{
		this.clientName = clientDisplayedName;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

}
