/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.base.entity.EntityParam;

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

	public void setMappedToExistingEntity(EntityParam mappedAtExistingEntity)
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

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (!authenticatedWith.isEmpty())
			sb.append("Authenticated with: ").append(authenticatedWith).append("\n");
		if (!identities.isEmpty())
		{
			sb.append("Identities:\n");
			for (MappedIdentity id: identities)
				sb.append(" - ").append(id).append("\n");
		}
		if (!entityChanges.isEmpty())
		{
			sb.append("Entity changes:\n");
			for (EntityChange id: entityChanges)
				sb.append(" - ").append(id).append("\n");
		}
		if (mappedAtExistingEntity != null)
			sb.append("Mapped at existing entity: ").append(mappedAtExistingEntity).append("\n");
		if (!attributes.isEmpty())
		{
			sb.append("Attributes:\n");
			for (MappedAttribute at: attributes)
				sb.append(" - ").append(at).append("\n");
		}
		if (!groups.isEmpty())
		{
			sb.append("Groups:\n");
			for (MappedGroup gr: groups)
				sb.append(" - ").append(gr).append("\n");
		}
		sb.append("Cleaning stale groups: ").append(cleanStaleGroups).append(" attributes: ")
			.append(cleanStaleAttributes).append(" identities: ").append(cleanStaleIdentities);
		return sb.toString();
	}
}
