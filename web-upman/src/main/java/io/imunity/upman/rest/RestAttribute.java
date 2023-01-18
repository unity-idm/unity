/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

class RestAttribute
{
	public final String name;
	public final List<String> values;

	@JsonCreator
	RestAttribute(@JsonProperty("name") String name, @JsonProperty("values") List<String> values)
	{
		this.name = name;
		this.values = values;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestAttribute that = (RestAttribute) o;
		return Objects.equals(name, that.name) && Objects.equals(values, that.values);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, values);
	}

	@Override
	public String toString()
	{
		return "RestAttribute{" +
			"name='" + name + '\'' +
			", values=" + values +
			'}';
	}
}
