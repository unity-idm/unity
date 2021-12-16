/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AttributePolicy
{
	public final String name;
	public final List<Attribute> attributes;
	public final List<String> targetIdps;
	public final List<String> targetFederations;

	public AttributePolicy()
	{
		this.name = null;
		this.attributes = null;
		this.targetFederations = null;
		this.targetIdps = null;
	}

	public AttributePolicy(String name, List<Attribute> attributes, List<String> targetIdps,
			List<String> targetFederations)
	{
		this.name = name;
		this.attributes = attributes != null ? Collections.unmodifiableList(attributes) : null;
		this.targetIdps = targetIdps != null ? Collections.unmodifiableList(targetIdps) : null;
		this.targetFederations = targetFederations != null ? Collections.unmodifiableList(targetFederations) : null;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, name, targetFederations, targetIdps);
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
		AttributePolicy other = (AttributePolicy) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(name, other.name)
				&& Objects.equals(targetFederations, other.targetFederations)
				&& Objects.equals(targetIdps, other.targetIdps);
	}
	
	@Override
	public String toString()
	{
		
		return "AttributePolicy[name = " + name + ", attributes=" + String.join(",", attributes.stream().map(a -> a.name).collect(Collectors.toList())) + "]"; 
	}

}
