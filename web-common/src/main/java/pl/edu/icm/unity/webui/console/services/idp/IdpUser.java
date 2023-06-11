/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.idp;

import pl.edu.icm.unity.base.identity.EntityState;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

/**
 * Contains information about single identity
 * 
 * @author P.Piernik
 *
 */
public class IdpUser implements FilterableEntry
{
	public final Long entity;
	public final String name;
	public final String group;
	public final EntityState state;

	public IdpUser(Long entity, String name, String group, EntityState state)
	{
		this.entity = entity;
		this.name = name;
		this.group = group;
		this.state = state;
	}

	@Override
	public boolean anyFieldContains(String searched, MessageSource msg)
	{
		String textLower = searched.toLowerCase();
		if (name != null && name.toLowerCase().contains(textLower))
			return true;
		if (state != null && state.toString().toLowerCase().contains(textLower))
			return true;

		return false;
	}
}
