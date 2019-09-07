/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.idp;

import java.util.List;

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

}
