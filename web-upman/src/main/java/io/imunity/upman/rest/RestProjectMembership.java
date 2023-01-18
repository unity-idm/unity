/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = RestProjectMembership.RestProjectMembershipBuilder.class)
class RestProjectMembership
{
	public final String email;
	public final String role;
	public final List<RestAttribute> attributes;

	RestProjectMembership(String email, String role, List<RestAttribute> attributes)
	{
		this.email = email;
		this.role = role;
		this.attributes = attributes;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestProjectMembership that = (RestProjectMembership) o;
		return Objects.equals(email, that.email) && Objects.equals(role, that.role) && Objects.equals(attributes,
			that.attributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(email, role, attributes);
	}

	@Override
	public String toString()
	{
		return "RestProjectMembership{" +
			"email='" + email + '\'' +
			", role='" + role + '\'' +
			", attributes=" + attributes +
			'}';
	}

	public static RestProjectMembershipBuilder builder()
	{
		return new RestProjectMembershipBuilder();
	}

	public static final class RestProjectMembershipBuilder
	{
		public String email;
		public String role;
		public List<RestAttribute> attributes;

		private RestProjectMembershipBuilder()
		{
		}

		public RestProjectMembershipBuilder withEmail(String email)
		{
			this.email = email;
			return this;
		}

		public RestProjectMembershipBuilder withRole(String role)
		{
			this.role = role;
			return this;
		}

		public RestProjectMembershipBuilder withAttributes(List<RestAttribute> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public RestProjectMembership build()
		{
			return new RestProjectMembership(email, role, attributes);
		}
	}
}
