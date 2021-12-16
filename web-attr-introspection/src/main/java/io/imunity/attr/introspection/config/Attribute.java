/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Attribute
{
	public final String name;
	public final String description;
	public final boolean mandatory;

	public Attribute(@JsonProperty("name") String name, @JsonProperty("description") String description,
			@JsonProperty("mandatory") Boolean mandatory)
	{
		this.name = name;
		this.mandatory = mandatory;
		this.description = description;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, mandatory, name);
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
		Attribute other = (Attribute) obj;
		return Objects.equals(description, other.description) && mandatory == other.mandatory
				&& Objects.equals(name, other.name);
	}
	
	
}
