/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.groupMember;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.identity.Identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class GroupMemberWithAttributes
{
	private final EntityInformation entityInformation;
	private final List<Identity> identities;
	private final Collection<AttributeExt> attributes;

	public GroupMemberWithAttributes(EntityInformation entityInformation, List<Identity> identities, Collection<AttributeExt> attributes)
	{
		this.entityInformation = entityInformation;
		this.identities = List.copyOf(identities);
		this.attributes = List.copyOf(attributes);
	}

	public EntityInformation getEntityInformation()
	{
		return entityInformation;
	}

	public List<Identity> getIdentities()
	{
		return identities;
	}

	public Collection<AttributeExt> getAttributes()
	{
		return new ArrayList<>(attributes);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GroupMemberWithAttributes that = (GroupMemberWithAttributes) o;
		return Objects.equals(entityInformation, that.entityInformation) &&
				Objects.equals(identities, that.identities) &&
				Objects.equals(attributes, that.attributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityInformation, identities, attributes);
	}

	@Override
	public String toString()
	{
		return "SimpleGroupMember{" +
				"entityInformation=" + entityInformation +
				", identities=" + identities +
				", attributes=" + attributes +
				'}';
	}
}
