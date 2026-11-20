/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ProjectMembership.Builder.class)
record ProjectMembership(
		String email,
		String role,
		List<MemberAttribute> attributes) implements Member
{
	ProjectMembership
	{
		attributes = List.copyOf(attributes);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String email;
		private String role;
		private List<MemberAttribute> attributes;

		private Builder()
		{
		}

		Builder withEmail(String email)
		{
			this.email = email;
			return this;
		}

		Builder withRole(String role)
		{
			this.role = role;
			return this;
		}

		Builder withAttributes(List<MemberAttribute> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		ProjectMembership build()
		{
			return new ProjectMembership(email, role, attributes);
		}
	}
}
