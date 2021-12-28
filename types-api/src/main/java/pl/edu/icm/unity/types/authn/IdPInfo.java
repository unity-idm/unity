/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.authn;

import static org.springframework.util.Assert.notNull;

import java.util.Objects;
import java.util.Optional;

import pl.edu.icm.unity.types.I18nString;

public class IdPInfo
{
	public final String id;
	public final Optional<String> configId;
	public final Optional<I18nString> displayedName;
	public final Optional<IdpGroup> group;

	private IdPInfo(Builder builder)
	{
		this.id = builder.id;
		this.configId = builder.configId;
		this.displayedName = builder.displayedName;
		this.group = builder.group;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(group, id, configId);
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
		IdPInfo other = (IdPInfo) obj;
		return Objects.equals(group, other.group) && Objects.equals(id, other.id)
				&& Objects.equals(configId, other.configId);
	}

	@Override
	public String toString()
	{
		return "IdPInfo [id=" + id
				+ (!displayedName.isEmpty() ? ", displayedName=" + displayedName.orElse(new I18nString()) : "")
				+ (!group.isEmpty() ? ", group=" + group.get() + ", maxElements=" : "") + ", configId="
				+ configId.orElse("") + "]";
	}

	public static class IdpGroup
	{
		public final String id;
		public final Optional<String> displayedName;

		public IdpGroup(String id, Optional<String> displayedName)
		{
			this.id = id;
			this.displayedName = displayedName;
		}

		@Override
		public String toString()
		{
			return "IdpGroup [id=" + id + (!displayedName.isEmpty() ? ", displayedName=" + displayedName.get() : "")
					+ "]";
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(id);
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
			IdpGroup other = (IdpGroup) obj;
			return Objects.equals(id, other.id);
		}

	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String id;
		private Optional<String> configId = Optional.empty();
		private Optional<I18nString> displayedName = Optional.empty();
		private Optional<IdpGroup> group = Optional.empty();

		private Builder()
		{
		}

		public Builder withId(String id)
		{
			this.id = id;
			return this;
		}

		public Builder withConfigId(String configId)
		{
			this.configId = Optional.ofNullable(configId);
			return this;
		}

		public Builder withDisplayedName(I18nString displayedName)
		{
			this.displayedName = Optional.ofNullable(displayedName);
			return this;
		}

		public Builder withGroup(IdpGroup group)
		{
			this.group = Optional.ofNullable(group);
			return this;
		}

		public IdPInfo build()
		{
			
			notNull(id, "id cannot be null.");
			return new IdPInfo(this);
		}
	}
}
