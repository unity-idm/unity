/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.types.basic.DelegatedGroupMember;
import pl.edu.icm.unity.types.basic.GroupAuthorizationRole;

/**
 * Data object behind a row in {@link GroupMemebersGrid}. Stores group member
 * information
 * 
 * @author P.Piernik
 *
 */
public class GroupMemberEntry
{
	private Map<String, String> attributes;
	private Long entityId;
	private String name;
	private String email;
	private GroupAuthorizationRole role;

	GroupMemberEntry(DelegatedGroupMember member, Map<String, String> attributes)
	{

		this.entityId = member.entityId;
		this.name = member.name;
		this.email = member.email;
		this.role = member.role;
		this.attributes = new HashMap<>(attributes);

	}

	public String getAttribute(String key)
	{
		return attributes.get(key);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((role == null) ? 0 : role.hashCode());
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

		if (entityId != other.getEntityId())
			return false;

		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		if (email == null)
		{
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;

		if (role == null)
		{
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;

		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;

		return true;
	}

	public long getEntityId()
	{
		return entityId;
	}

	public GroupAuthorizationRole getRole()
	{
		return role;
	}

	public String getName()
	{
		return name;
	}

	public String getEmail()
	{
		return email;
	}
}
