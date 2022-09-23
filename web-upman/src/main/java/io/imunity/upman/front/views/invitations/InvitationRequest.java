/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

class InvitationRequest
{
	public final ProjectGroup projectGroup;
	public Set<String> emails;
	public Set<GroupTreeNode> groups;
	public boolean allowModifyGroups;
	public Instant expiration;

	public InvitationRequest(ProjectGroup projectGroup)
	{
		this.projectGroup = projectGroup;
	}

	public InvitationRequest(ProjectGroup projectGroup, Set<String> emails, Set<GroupTreeNode> groups, boolean allowModifyGroups, Instant expiration)
	{
		this.projectGroup = projectGroup;
		this.emails = emails;
		this.groups = groups;
		this.allowModifyGroups = allowModifyGroups;
		this.expiration = expiration;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InvitationRequest that = (InvitationRequest) o;
		return allowModifyGroups == that.allowModifyGroups &&
				Objects.equals(projectGroup, that.projectGroup) &&
				Objects.equals(emails, that.emails) &&
				Objects.equals(groups, that.groups) &&
				Objects.equals(expiration, that.expiration);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(projectGroup, emails, groups, allowModifyGroups, expiration);
	}

	@Override
	public String toString()
	{
		return "InvitationP{" +
				"projectGroup=" + projectGroup +
				", emails=" + emails +
				", groups=" + groups +
				", allowModifyGroups=" + allowModifyGroups +
				", expiration=" + expiration +
				'}';
	}
}
