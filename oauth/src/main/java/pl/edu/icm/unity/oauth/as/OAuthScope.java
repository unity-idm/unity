/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class OAuthScope
{
	public final String name;
	public final String description;
	public final List<String> attributes;
	public final boolean enabled;

	private OAuthScope(Builder builder)
	{
		this.name = builder.name;
		this.description = builder.description;
		this.attributes = List.copyOf(builder.attributes);
		this.enabled = builder.enabled;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, description, enabled, name);
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
		OAuthScope other = (OAuthScope) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(description, other.description)
				&& enabled == other.enabled && Objects.equals(name, other.name);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private List<String> attributes = Collections.emptyList();
		private boolean enabled;

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

		public Builder withAttributes(List<String> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public Builder withEnabled(boolean enabled)
		{
			this.enabled = enabled;
			return this;
		}

		public OAuthScope build()
		{
			return new OAuthScope(this);
		}
	}

}