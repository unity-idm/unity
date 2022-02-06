/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.group;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.scim.common.BasicSCIMResource;

@JsonDeserialize(builder = SCIMGroupResource.Builder.class)
class SCIMGroupResource extends BasicSCIMResource
{
	static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";

	public final String displayName;
	public final List<SCIMGroupMemberResource> members;

	private SCIMGroupResource(Builder builder)
	{
		super(builder);
		this.displayName = builder.displayName;
		this.members = List.copyOf(builder.members);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(displayName, members);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SCIMGroupResource other = (SCIMGroupResource) obj;
		return Objects.equals(displayName, other.displayName) && Objects.equals(members, other.members);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private String displayName;
		private List<SCIMGroupMemberResource> members = Collections.emptyList();

		private Builder()
		{
			withSchemas(Set.of(SCHEMA));
		}

		public Builder withDisplayName(String displayName)
		{
			this.displayName = displayName;
			return this;
		}

		public Builder withMembers(List<SCIMGroupMemberResource> members)
		{
			this.members = members;
			return this;
		}

		public SCIMGroupResource build()
		{
			return new SCIMGroupResource(this);
		}
	}

}
