/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;


@JsonDeserialize(builder = RestGroupContents.Builder.class)
@JsonInclude(Include.NON_NULL)
public class RestGroupContents
{
	public final RestGroup group;
	public final List<String> subGroups;
	public final List<RestGroupMembership> members;

	private RestGroupContents(Builder builder)
	{
		this.group = builder.group;
		this.subGroups = builder.subGroups;
		this.members = builder.members;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(group, members, subGroups);
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
		RestGroupContents other = (RestGroupContents) obj;
		return Objects.equals(group, other.group) && Objects.equals(members, other.members)
				&& Objects.equals(subGroups, other.subGroups);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private RestGroup group;
		private List<String> subGroups = Collections.emptyList();
		private List<RestGroupMembership> members = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withGroup(RestGroup group)
		{
			this.group = group;
			return this;
		}

		public Builder withSubGroups(List<String> subGroups)
		{
			this.subGroups = subGroups;
			return this;
		}

		public Builder withMembers(List<RestGroupMembership> members)
		{
			this.members = members;
			return this;
		}

		public RestGroupContents build()
		{
			return new RestGroupContents(this);
		}
	}

}
