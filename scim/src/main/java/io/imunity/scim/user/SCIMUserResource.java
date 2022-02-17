/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.scim.common.BasicSCIMResource;
import io.imunity.scim.scheme.DefaultSchemaProvider;

@JsonDeserialize(builder = SCIMUserResource.Builder.class)
class SCIMUserResource extends BasicSCIMResource
{
	static final String SCHEMA = DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID;

	public final String userName;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public final List<SCIMUserGroupResource> groups;

	private SCIMUserResource(Builder builder)
	{
		super(builder);
		this.userName = builder.userName;
		this.groups = List.copyOf(builder.groups);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(groups, userName);
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
		SCIMUserResource other = (SCIMUserResource) obj;
		return Objects.equals(groups, other.groups) && Objects.equals(userName, other.userName);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private String userName;
		private List<SCIMUserGroupResource> groups = Collections.emptyList();

		public Builder()
		{
			withSchemas(Set.of(SCHEMA));
		}

		public Builder withUserName(String userName)
		{
			this.userName = userName;
			return this;
		}

		public Builder withGroups(List<SCIMUserGroupResource> groups)
		{
			this.groups = groups;
			return this;
		}

		public SCIMUserResource build()
		{
			return new SCIMUserResource(this);
		}
	}

}
