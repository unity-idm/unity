/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Complete info about entity: its identities and relevant attributes.
 * Used to populate table.
 */
class ResolvedEntity
{
	private Entity entity;
	private Set<Identity> identities;
	private Map<String, ? extends Attribute> rootAttributes;
	private Map<String, ? extends Attribute> currentAttributes;

	public ResolvedEntity(Entity entity, List<Identity> identities, 
			Map<String, ? extends Attribute> rootAttributes, Map<String, ? extends Attribute> currentAttributes)
	{
		this.identities = new LinkedHashSet<>(identities); 
		this.rootAttributes = rootAttributes;
		this.currentAttributes = currentAttributes;
		this.entity = entity;
	}
	
	public void removeIdentity(Identity identity)
	{
		identities.remove(identity);
	}

	public Collection<Identity> getIdentities()
	{
		return identities;
	}

	public Map<String, ? extends Attribute> getRootAttributes()
	{
		return rootAttributes;
	}

	public Map<String, ? extends Attribute> getCurrentAttributes()
	{
		return currentAttributes;
	}

	public Entity getEntity()
	{
		return entity;
	}
}