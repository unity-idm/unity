/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class ReceivedAttribute
{
	final String name;
	final Optional<String> description;
	final List<Object> values;

	ReceivedAttribute(String name, Optional<String> description, List<Object> values)
	{
		this.name = name;
		this.description = description;
		this.values = values != null ? Collections.unmodifiableList(values) : null;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(description, name, values);
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
		ReceivedAttribute other = (ReceivedAttribute) obj;
		return Objects.equals(description, other.description) && Objects.equals(name, other.name)
				&& Objects.equals(values, other.values);
	}

}