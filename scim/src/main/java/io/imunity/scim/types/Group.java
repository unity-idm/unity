/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.types;

import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = Group.Builder.class)
public class Group
{
	public enum GroupType
	{
		direct, indirect
	}

	public final String value;
	@JsonProperty("$ref")
	public final URI ref;
	public final String display;
	public final GroupType type;

	private Group(Builder builder)
	{
		this.value = builder.value;
		this.ref = builder.ref;
		this.display = builder.display;
		this.type = builder.type;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(display, ref, type, value);
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
		Group other = (Group) obj;
		return Objects.equals(display, other.display) && Objects.equals(ref, other.ref) && type == other.type
				&& Objects.equals(value, other.value);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String value;
		private URI ref;
		private String display;
		private GroupType type;

		private Builder()
		{
		}

		public Builder withValue(String value)
		{
			this.value = value;
			return this;
		}

		public Builder withRef(URI ref)
		{
			this.ref = ref;
			return this;
		}

		public Builder withDisplay(String display)
		{
			this.display = display;
			return this;
		}

		public Builder withType(GroupType type)
		{
			this.type = type;
			return this;
		}

		public Group build()
		{
			return new Group(this);
		}
	}
}
