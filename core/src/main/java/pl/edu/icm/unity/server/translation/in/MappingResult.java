/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Stores a set of mapping results, produced by one or more translation actions.
 * @author K. Benedyczak
 */
public class MappingResult
{
	private List<MappedGroup> groups = new ArrayList<>();
	private List<MappedIdentity> identities = new ArrayList<>();
	private List<MappedAttribute> attributes = new ArrayList<>();
	private List<EntityChange> entityChanges = new ArrayList<>();
	private EntityParam mappedAtExistingEntity;
	private Set<String> authenticatedWith = new HashSet<>();
	private boolean cleanStaleGroups;
	private boolean cleanStaleAttributes;
	private boolean cleanStaleIdentities;
	
	public MappingResult()
	{
	}
	
	public void addGroup(MappedGroup group)
	{
		groups.add(group);
	}
	
	public void addIdentity(MappedIdentity id)
	{
		identities.add(id);
	}
	
	public void addAttribute(MappedAttribute attr)
	{
		attributes.add(attr);
	}

	public void addEntityChange(EntityChange change)
	{
		entityChanges.add(change);
	}
	
	public List<MappedGroup> getGroups()
	{
		return groups;
	}

	public List<MappedIdentity> getIdentities()
	{
		return identities;
	}

	public List<MappedAttribute> getAttributes()
	{
		return attributes;
	}
	
	public List<EntityChange> getEntityChanges()
	{
		return entityChanges;
	}

	public boolean isCleanStaleGroups()
	{
		return cleanStaleGroups;
	}

	public void setCleanStaleGroups(boolean cleanStaleGroups)
	{
		this.cleanStaleGroups = cleanStaleGroups;
	}

	public boolean isCleanStaleAttributes()
	{
		return cleanStaleAttributes;
	}

	public void setCleanStaleAttributes(boolean cleanStaleAttributes)
	{
		this.cleanStaleAttributes = cleanStaleAttributes;
	}

	public boolean isCleanStaleIdentities()
	{
		return cleanStaleIdentities;
	}

	public void setCleanStaleIdentities(boolean cleanStaleIdentities)
	{
		this.cleanStaleIdentities = cleanStaleIdentities;
	}
	
	/**
	 * @return null if remote entity was not mapped on a local one or the local principal
	 */
	public EntityParam getMappedAtExistingEntity()
	{
		return mappedAtExistingEntity;
	}

	public void setMappedAtExistingEntity(EntityParam mappedAtExistingEntity)
	{
		this.mappedAtExistingEntity = mappedAtExistingEntity;
	}

	public Set<String> getAuthenticatedWith()
	{
		return authenticatedWith;
	}

	public void addAuthenticatedWith(String authenticatedWith)
	{
		this.authenticatedWith.add(authenticatedWith);
	}

	public void mergeWith(MappingResult result)
	{
		groups.addAll(result.getGroups());
		identities.addAll(result.getIdentities());
		attributes.addAll(result.getAttributes());
		entityChanges.addAll(result.getEntityChanges());
		this.cleanStaleAttributes |= result.isCleanStaleAttributes();
		this.cleanStaleIdentities |= result.isCleanStaleIdentities();
		this.cleanStaleGroups |= result.isCleanStaleGroups();
		if (mappedAtExistingEntity == null)
			this.mappedAtExistingEntity = result.mappedAtExistingEntity; 
	}
}
