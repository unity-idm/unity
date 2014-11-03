/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores a set of mapping results, produced by one or more translation actions.
 * @author K. Benedyczak
 */
public class MappingResult
{
	private List<MappedGroup> groups = new ArrayList<MappedGroup>();
	private List<MappedIdentity> identities = new ArrayList<MappedIdentity>();
	private List<MappedAttribute> attributes = new ArrayList<MappedAttribute>();
	
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
	
	public void mergeWith(MappingResult result)
	{
		groups.addAll(result.getGroups());
		identities.addAll(result.getIdentities());
		attributes.addAll(result.getAttributes());
	}
}
