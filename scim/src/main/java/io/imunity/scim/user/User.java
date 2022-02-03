/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class User
{
	final Long entityId;
	final List<UserIdentity> identities;
	final Set<UserGroup> groups;

	private User(Builder builder)
	{
		this.entityId = builder.entityId;
		this.identities = builder.identities;
		this.groups = builder.groups;
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
