/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.idp;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityState;
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
	public final String identity;
	public final String identityType;
	public final EntityState state;

	public IdpUser(Long entity, String name, String group, String identity, String identityType, EntityState state)
	{
		this.entity = entity;
		this.name = name;
		this.group = group;
		this.identity = identity;
		this.identityType = identityType;
		this.state = state;
	}

	@Override
	public boolean anyFieldContains(String searched, UnityMessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (name != null && name.toLowerCase().contains(textLower))
			return true;

		if (identity != null && identity.toLowerCase().contains(textLower))
			return true;

		if (identityType != null && identityType.toLowerCase().contains(textLower))
			return true;

		if (state != null && state.toString().toLowerCase().contains(textLower))
			return true;

		return false;
	}
}
