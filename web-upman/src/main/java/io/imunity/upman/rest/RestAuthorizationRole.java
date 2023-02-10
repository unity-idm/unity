/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

class RestAuthorizationRole
{
	final String role;
	@JsonCreator
	RestAuthorizationRole(@JsonProperty("role") String role)
	{
		this.role = role;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestAuthorizationRole that = (RestAuthorizationRole) o;
		return Objects.equals(role, that.role);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(role);
	}

	@Override
	public String toString()
	{
		return "RestAuthorizationRole{" +
			"role='" + role + '\'' +
			'}';
	}
}
