/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.model;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class RestGroupMemberWithAttributes
{
	private RestEntityInformation entityInformation;
	private List<RestIdentity> identities;
	private Collection<RestAttributeExt> attributes;

	public RestGroupMemberWithAttributes(RestEntityInformation entityInformation, List<RestIdentity> identities, Collection<RestAttributeExt> attributes)
	{
		this.entityInformation = entityInformation;
		this.identities = List.copyOf(identities);
		this.attributes = List.copyOf(attributes);
	}

	//for Jackson
	protected RestGroupMemberWithAttributes()
	{
	}

	public RestEntityInformation getEntityInformation()
	{
		return entityInformation;
	}

	public List<RestIdentity> getIdentities()
	{
		return identities;
	}

	public Collection<RestAttributeExt> getAttributes()
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
