/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.Collections;
import java.util.Map;

import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Hidden implementation of the data backing bulk operations on group structures.
 * 
 * @author K. Benedyczak
 */
class GroupStructuralDataImpl implements GroupStructuralData
{
	private Map<String, Group> groups;
	private String group;
	
	private GroupStructuralDataImpl() 
	{
	}
	
	public static Builder builder()
	{
		return new Builder();
	}
	
	public Map<String, Group> getGroups()
	{
		return groups;
	}

	public String getGroup()
	{
		return group;
	}

	public static class Builder
	{
		GroupStructuralDataImpl obj = new GroupStructuralDataImpl();
		
		public Builder withGroups(Map<String, Group> groups)
		{
			obj.groups = Collections.unmodifiableMap(groups);
			return this;
		}

		public Builder withGroup(String group)
		{
			obj.group = group;
			return this;
		}
		
		public GroupStructuralDataImpl build()
		{
			GroupStructuralDataImpl ret = obj;
			obj = new GroupStructuralDataImpl();
			return ret;
		}
	}
}
