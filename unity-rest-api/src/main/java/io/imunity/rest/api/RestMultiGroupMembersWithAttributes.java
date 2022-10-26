/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;

/**
 * Provides details about members of groups.
 */

@JsonDeserialize(builder = RestMultiGroupMembersWithAttributes.Builder.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RestMultiGroupMembersWithAttributes
{
	public final Map<String, List<RestGroupMemberWithAttributes>> members;

	private RestMultiGroupMembersWithAttributes(Builder builder)
	{
		this.members = builder.members;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(members);
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
		RestMultiGroupMembersWithAttributes other = (RestMultiGroupMembersWithAttributes) obj;
		return Objects.equals(members, other.members);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Map<String, List<RestGroupMemberWithAttributes>> members = Collections.emptyMap();

		private Builder()
		{
		}

		public Builder withMembers(Map<String, List<RestGroupMemberWithAttributes>> members)
		{
			this.members = members;
			return this;
		}

		public RestMultiGroupMembersWithAttributes build()
		{
			return new RestMultiGroupMembersWithAttributes(this);
		}
	}

}
