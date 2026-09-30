/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.api;

import java.util.Objects;

public class Scope
{
	public final String name;
	public final String description;
	public final boolean pattern;

	@Override
	public int hashCode()
	{
		return Objects.hash(description, name, pattern);
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
		Scope other = (Scope) obj;
		return Objects.equals(description, other.description) && Objects.equals(name, other.name)
				&& pattern == other.pattern;
	}

	private Scope(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.pattern = builder.pattern;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private boolean pattern;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withPattern(boolean pattern)
		{
			this.pattern = pattern;
			return this;
		}

		public Scope build()
		{
			return new Scope(this);
		}
	}

}
