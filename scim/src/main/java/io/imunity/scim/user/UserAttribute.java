/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.List;
import java.util.Objects;
import java.util.Collections;

class UserAttribute
{
	final String name;
	final List<String> values;

	private UserAttribute(Builder builder)
	{
		this.name = builder.name;
		this.values = List.copyOf(builder.values);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, values);
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
		UserAttribute other = (UserAttribute) obj;
		return Objects.equals(name, other.name) && Objects.equals(values, other.values);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String name;
		private List<String> values = Collections.emptyList();

		private Builder()
		{
		}

		Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		Builder withValues(List<String> values)
		{
			this.values = values;
			return this;
		}

		UserAttribute build()
		{
			return new UserAttribute(this);
		}
	}

}
