/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.idp;

import java.util.List;
import java.util.Objects;

/**
 * Represent single active value configuration in idp service
 * 
 * @author P.Piernik
 *
 */
public class ActiveValueConfig
{
	private String clientId;
	private List<String> singleSelectableAttributes;
	private List<String> multiSelectableAttributes;

	public ActiveValueConfig()
	{

	}

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	public List<String> getSingleSelectableAttributes()
	{
		return singleSelectableAttributes;
	}

	public void setSingleSelectableAttributes(List<String> singleSelectableAttributes)
	{
		this.singleSelectableAttributes = singleSelectableAttributes;
	}

	public List<String> getMultiSelectableAttributes()
	{
		return multiSelectableAttributes;
	}

	public void setMultiSelectableAttributes(List<String> multiSelectableAttributes)
	{
		this.multiSelectableAttributes = multiSelectableAttributes;
	}

	public ActiveValueConfig clone() 
	{
		ActiveValueConfig clone = new ActiveValueConfig();
		clone.setClientId(clientId);
		clone.setMultiSelectableAttributes(multiSelectableAttributes);
		clone.setSingleSelectableAttributes(singleSelectableAttributes);
		return clone;
	}
	
	
	@Override
	public int hashCode()
	{
		return Objects.hash(clientId, multiSelectableAttributes, singleSelectableAttributes);
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
		ActiveValueConfig other = (ActiveValueConfig) obj;
		return Objects.equals(clientId, other.clientId)
				&& Objects.equals(multiSelectableAttributes, other.multiSelectableAttributes)
				&& Objects.equals(singleSelectableAttributes, other.singleSelectableAttributes);
	}
	
	

}
