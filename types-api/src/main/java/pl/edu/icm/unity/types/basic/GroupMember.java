/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Entity as group member. Provides information on {@link Entity} and its all attributes in some group.
 *  
 * @author K. Benedyczak
 */
public class GroupMember
{
	private String group;
	private Entity entity;
	private Collection<AttributeExt> attributes;
	
	public GroupMember(String group, Entity entity, Collection<AttributeExt> attributes)
	{
		this.group = group;
		this.entity = entity;
		this.attributes = attributes;
	}

	//for Jackson
	protected GroupMember()
	{
	}
	
	public String getGroup()
	{
		return group;
	}

	public Entity getEntity()
	{
		return entity;
	}

	public Collection<AttributeExt> getAttributes()
	{
		return new ArrayList<>(attributes);
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof GroupMember))
			return false;
		GroupMember castOther = (GroupMember) other;
		return Objects.equals(group, castOther.group) && Objects.equals(entity, castOther.entity)
				&& Objects.equals(attributes, castOther.attributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(group, entity, attributes);
	}

	@Override
	public String toString()
	{
		return "GroupMember [group=" + group + ", entity=" + entity + ", attributes=" + attributes + "]";
	}
}
