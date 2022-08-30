/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.admin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = MembershipGroupsConfiguration.Builder.class)
class MembershipGroupsConfiguration
{
	final Optional<List<String>> membershipGroups;
	final Optional<List<String>> excludedMemberhipGroups;

	private MembershipGroupsConfiguration(Builder builder)
	{
		this.membershipGroups = builder.membershipGroups;
		this.excludedMemberhipGroups = builder.excludedMemberhipGroups;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(excludedMemberhipGroups, membershipGroups);
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
		MembershipGroupsConfiguration other = (MembershipGroupsConfiguration) obj;
		return Objects.equals(excludedMemberhipGroups, other.excludedMemberhipGroups)
				&& Objects.equals(membershipGroups, other.membershipGroups);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Optional<List<String>> membershipGroups = Optional.empty();
		private Optional<List<String>> excludedMemberhipGroups = Optional.empty();

		private Builder()
		{
		}

		public Builder withMembershipGroups(Optional<List<String>> membershipGroups)
		{
			this.membershipGroups = membershipGroups;
			return this;
		}

		public Builder withExcludedMemberhipGroups(Optional<List<String>> excludedMemberhipGroups)
		{
			this.excludedMemberhipGroups = excludedMemberhipGroups;
			return this;
		}

		public MembershipGroupsConfiguration build()
		{
			return new MembershipGroupsConfiguration(this);
		}
	}
}
