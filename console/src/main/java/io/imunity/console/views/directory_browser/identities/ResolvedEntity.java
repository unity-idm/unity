/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.identity.Identity;

import java.util.*;


class ResolvedEntity
{
	private final Entity entity;
	private final Set<Identity> identities;
	private final Map<String, ? extends Attribute> rootAttributes;
	private final Map<String, ? extends Attribute> currentAttributes;

	ResolvedEntity(Entity entity, List<Identity> identities, 
			Map<String, ? extends Attribute> rootAttributes, Map<String, ? extends Attribute> currentAttributes)
	{
		this.identities = new LinkedHashSet<>(identities); 
		this.rootAttributes = rootAttributes;
		this.currentAttributes = currentAttributes;
		this.entity = entity;
	}

	Collection<Identity> getIdentities()
	{
		return identities;
	}

	Map<String, ? extends Attribute> getRootAttributes()
	{
		return rootAttributes;
	}

	Map<String, ? extends Attribute> getCurrentAttributes()
	{
		return currentAttributes;
	}

	Entity getEntity()
	{
		return entity;
	}
}