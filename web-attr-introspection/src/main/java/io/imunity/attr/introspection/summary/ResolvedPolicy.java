/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.attr.introspection.config.Attribute;

class ResolvedPolicy
{
	final Optional<String> name;
	final List<Attribute> attributes;

	ResolvedPolicy(Optional<String> name, List<Attribute> attributes)
	{
		this.name = name;
		this.attributes = attributes != null ? Collections.unmodifiableList(attributes) : null;
	}

	List<Attribute> getMandatoryAttributes()
	{
		return attributes.stream().filter(a -> a.mandatory).collect(Collectors.toList());
	}

	List<Attribute> getOptionalAttributes()
	{
		return attributes.stream().filter(a -> !a.mandatory).collect(Collectors.toList());
	}
}