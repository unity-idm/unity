/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.admin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = MembershipGroupsConfiguration.Builder.class)
public class MembershipGroupsConfiguration
{
	public final List<String> membershipGroups;
	public final List<String> excludedMemberhipGroups;

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
		private List<String> membershipGroups = Collections.emptyList();
		private List<String> excludedMemberhipGroups = Collections.emptyList();

		private Builder()
		{
		}

		public Builder withMembershipGroups(List<String> membershipGroups)
		{
			this.membershipGroups = membershipGroups;
			return this;
		}

		public Builder withExcludedMemberhipGroups(List<String> excludedMemberhipGroups)
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
