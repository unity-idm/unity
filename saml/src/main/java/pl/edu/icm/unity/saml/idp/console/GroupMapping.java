/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;

/**
 * Contains SAML group mapping information
 * 
 * @author P.Piernik
 *
 */
public class GroupMapping
{
	private String clientId;
	private GroupWithIndentIndicator group;

	public GroupMapping()
	{
	}

	public GroupWithIndentIndicator getGroup()
	{
		return group;
	}

	public void setGroup(GroupWithIndentIndicator group)
	{
		this.group = group;
	}

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}
}
