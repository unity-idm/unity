/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class User
{
	final Long entityId;
	final List<UserIdentity> identities;
	final Set<UserGroup> groups;

	private User(Builder builder)
	{
		this.entityId = builder.entityId;
		this.identities = List.copyOf(builder.identities);
		this.groups = Set.copyOf(builder.groups);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, groups, identities);
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
		return Objects.equals(entityId, other.entityId) && Objects.equals(groups, other.groups)
				&& Objects.equals(identities, other.identities);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private Long entityId;
		private List<UserIdentity> identities = Collections.emptyList();
		private Set<UserGroup> groups = Collections.emptySet();

		private Builder()
		{
		}

		Builder withEntityId(Long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		Builder withIdentities(List<UserIdentity> identities)
		{
			this.identities = identities;
			return this;
		}

		Builder withGroups(Set<UserGroup> groups)
		{
			this.groups = groups;
			return this;
		}

		User build()
		{
			return new User(this);
		}
	}

}
