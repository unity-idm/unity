/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.rest;

import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Entity as group member. Provides information on {@link Entity} and its all attributes in some group.
 */
public class RestGroupMemberWithAttributes
{
	private EntityInformation entityInformation;
	private List<Identity> identities;
	private Collection<AttributeExt> attributes;

	public RestGroupMemberWithAttributes(EntityInformation entityInformation, List<Identity> identities, Collection<AttributeExt> attributes)
	{
		this.entityInformation = entityInformation;
		this.identities = List.copyOf(identities);
		this.attributes = List.copyOf(attributes);
	}

	//for Jackson
	protected RestGroupMemberWithAttributes()
	{
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
		RestGroupMemberWithAttributes that = (RestGroupMemberWithAttributes) o;
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
