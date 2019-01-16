/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.imunity.upman.common.FilterableEntry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;

/**
 * Data object behind a row in {@link GroupMemebersGrid}. Stores group member
 * information
 * 
 * @author P.Piernik
 *
 */
class GroupMemberEntry implements FilterableEntry
{
	private Map<String, String> attributes;
	private DelegatedGroupMember member;

	public GroupMemberEntry(DelegatedGroupMember member, Map<String, String> attributes)
	{

		this.member = member;
		this.attributes = new HashMap<>();
		if (attributes != null)
		{
			this.attributes.putAll(attributes);
		}
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.member, this.attributes);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final GroupMemberEntry other = (GroupMemberEntry) obj;
		return Objects.equals(this.member, other.member) && Objects.equals(this.attributes, other.attributes);
	}

	public Map<String, String> getAttributes()
	{
		return attributes;
	}

	public long getEntityId()
	{
		return member.entityId;
	}

	public GroupAuthorizationRole getRole()
	{
		return member.role;
	}

	public String getName()
	{
		return member.name;
	}

	public VerifiableElementBase getEmail()
	{
		return member.email;
	}

	@Override
	public boolean anyFieldContains(String searched, UnityMessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (getRole() != null && msg.getMessage("Role." + getRole().toString().toLowerCase()).toLowerCase()
				.contains(textLower))
			return true;

		if (getName() != null && getName().toString().toLowerCase().contains(textLower))
			return true;
		if (getEmail() != null && getEmail().getValue().toLowerCase().contains(textLower))
			return true;

		for (Map.Entry<String, String> value : attributes.entrySet())

			if (value != null && value.getValue().toLowerCase().contains(textLower))
				return true;
		return false;
	}
}
