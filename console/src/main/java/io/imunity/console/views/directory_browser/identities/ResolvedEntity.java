/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.identity.Identity;

import java.util.*;


class ResolvedEntity
{
	private final Entity entity;
	private final Set<Identity> identities;

	ResolvedEntity(Entity entity, List<Identity> identities)
	{
		this.identities = new LinkedHashSet<>(identities);
		this.entity = entity;
	}

	Collection<Identity> getIdentities()
	{
		return identities;
	}

	Entity getEntity()
	{
		return entity;
	}
}
