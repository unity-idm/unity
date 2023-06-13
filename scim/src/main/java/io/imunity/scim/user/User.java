/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;

public class User
{
	final Long entityId;
	public final List<Identity> identities;
	public final Set<Group> groups;
	public final List<AttributeExt> attributes;

	private User(Builder builder)
	{
		this.entityId = builder.entityId;
		this.identities = List.copyOf(builder.identities);
		this.groups = Set.copyOf(builder.groups);
		this.attributes = List.copyOf(builder.attributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, entityId, groups, identities);
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
		User other = (User) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(entityId, other.entityId)
				&& Objects.equals(groups, other.groups) && Objects.equals(identities, other.identities);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Long entityId;
		private List<Identity> identities = Collections.emptyList();
		private Set<Group> groups = Collections.emptySet();
		private List<AttributeExt> attributes = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withEntityId(Long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public Builder withIdentities(List<Identity> identities)
		{
			this.identities = identities;
			return this;
		}

		public Builder withGroups(Set<Group> groups)
		{
			this.groups = groups;
			return this;
		}

		public Builder withAttributes(List<AttributeExt> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public User build()
		{
			return new User(this);
		}
	}

	
}
