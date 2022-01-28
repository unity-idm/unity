/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.types;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = UserResource.Builder.class)
public class UserResource extends BasicScimResource
{
	public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

	public final String userName;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public final List<Group> groups;

	private UserResource(Builder builder)
	{
		super(builder);
		this.userName = builder.userName;
		this.groups = builder.groups;
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
		UserResource other = (UserResource) obj;
		return Objects.equals(groups, other.groups) && Objects.equals(userName, other.userName);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends BasicScimResourceBuilder<Builder>
	{
		private String userName;
		private List<Group> groups = Collections.emptyList();

		public Builder()
		{
			withSchemas(Schemas.of(SCHEMA));
		}

		public Builder withUserName(String userName)
		{
			this.userName = userName;
			return this;
		}

		public Builder withGroups(List<Group> groups)
		{
			this.groups = groups;
			return this;
		}

		public UserResource build()
		{
			return new UserResource(this);
		}
	}

}
