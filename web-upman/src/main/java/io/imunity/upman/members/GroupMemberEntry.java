/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.HashMap;
import java.util.Map;

import io.imunity.upman.members.GroupMemebersGrid.BaseColumn;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * /** Data object behind a row in {@link GroupMemebersGrid}. Stores group
 * member information
 * 
 * @author P.Piernik
 *
 */
public class GroupMemberEntry
{
	public enum Role {regular, admin};
	
	private Map<BaseColumn, String> columnsToValues = new HashMap<>();
	private Map<String, String> attributes;

	private Role role;
	private String name;
	private String email;
	private String group;
	private long entityId;

	GroupMemberEntry(long entityId, String group, Map<String, String> attributes, Role role,
			String name, String email)
	{
		
		this.entityId = entityId;
		this.group = group;
		this.name = name;
		this.role = role;
		this.email = email;

		columnsToValues.put(BaseColumn.name, name);
		columnsToValues.put(BaseColumn.email, email);
		columnsToValues.put(BaseColumn.role, role.toString());

		this.attributes = new HashMap<>(attributes);

	}

	public String getAttribute(String key)
	{
		return attributes.get(key);
	}

	public String getBaseValue(BaseColumn key)
	{
		return columnsToValues.get(key);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result
				+ ((columnsToValues == null) ? 0 : columnsToValues.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroupMemberEntry other = (GroupMemberEntry) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (columnsToValues == null)
		{
			if (other.columnsToValues != null)
				return false;
		} else if (!columnsToValues.equals(other.columnsToValues))
			return false;
		return true;
	}

	public Role getRole()
	{
		return role;
	}

}
