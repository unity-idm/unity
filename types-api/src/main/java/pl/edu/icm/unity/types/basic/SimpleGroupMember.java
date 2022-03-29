/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Entity as group member. Provides information on {@link Entity} and its all attributes in some group.
 */
public class SimpleGroupMember
{
	private EntityInformation entityInformation;
	private List<Identity> identities;
	private Collection<AttributeExt> attributes;

	public SimpleGroupMember(EntityInformation entityInformation, List<Identity> identities, Collection<AttributeExt> attributes)
	{
		this.entityInformation = entityInformation;
		this.identities = identities;
		this.attributes = attributes;
	}

	//for Jackson
	protected SimpleGroupMember()
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
		SimpleGroupMember that = (SimpleGroupMember) o;
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
