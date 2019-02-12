/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;

/**
 * Holds information about delegated group member.
 * 
 * @author P.Piernik
 *
 */
public class DelegatedGroupMember
{
	public final long entityId;
	public final String project;
	public final String group;
	public final GroupAuthorizationRole role;
	public final String name;
	public final VerifiableElementBase email;
	public final List<Attribute> attributes;

	public DelegatedGroupMember(long entityId, String project, String group, GroupAuthorizationRole role,
			String name, VerifiableElementBase email, Optional<List<Attribute>> attributes)
	{
		this.entityId = entityId;
		this.project = project;
		this.group = group;
		this.role = role;
		this.name = name;
		this.email = email;
		this.attributes = !attributes.isPresent() ? Collections.unmodifiableList(Collections.emptyList())
				: Collections.unmodifiableList(attributes.get());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.entityId, this.project, this.group, this.role, this.name, this.email,
				this.attributes);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final DelegatedGroupMember other = (DelegatedGroupMember) obj;
		return Objects.equals(this.entityId, other.entityId) && Objects.equals(this.project, other.project)
				&& Objects.equals(this.group, other.group) && Objects.equals(this.role, other.role)
				&& Objects.equals(this.name, other.name) && Objects.equals(this.email, other.email)
				&& Objects.equals(this.attributes, other.attributes);
	}
}
