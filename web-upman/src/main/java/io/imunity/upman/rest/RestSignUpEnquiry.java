/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

class RestSignUpEnquiry
{
	public final String name;
	public final boolean autogenerate;

	@JsonCreator
	RestSignUpEnquiry(@JsonProperty("name") String name, @JsonProperty("autogenerate") boolean autogenerate)
	{
		this.name = name;
		this.autogenerate = autogenerate;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestSignUpEnquiry that = (RestSignUpEnquiry) o;
		return autogenerate == that.autogenerate && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, autogenerate);
	}

	@Override
	public String toString()
	{
		return "RestRegistrationForm{" +
			"name='" + name + '\'' +
			", autogenerate=" + autogenerate +
			'}';
	}
}
