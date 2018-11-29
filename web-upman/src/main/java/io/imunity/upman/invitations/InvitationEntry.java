/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.time.Instant;
import java.util.List;

import com.google.common.base.Objects;

import pl.edu.icm.unity.engine.api.project.ProjectInvitation;

/***
 * Data object behind a row in {@link InvitationsGrid}. Stores invitation
 * information
 * 
 * @author P.Piernik
 *
 */
class InvitationEntry
{
	public final String email;
	public final List<String> groups;
	public final Instant requestedTime;
	public final Instant expirationTime;
	public final String link;
	public final String code;

	public InvitationEntry(ProjectInvitation invitation)
	{
		this(invitation.getRegistrationCode(), invitation.getContactAddress(), invitation.getGroupPaths(),
				invitation.getLastSentTime(), invitation.getExpiration(), invitation.getLink());
	}

	public InvitationEntry(String code, String email, List<String> groups, Instant requestedTime,
			Instant expirationTime, String link)
	{
		this.code = code;
		this.email = email;
		this.groups = groups;
		this.requestedTime = requestedTime;
		this.expirationTime = expirationTime;
		this.link = link;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(code, email, groups, requestedTime, expirationTime, link);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final InvitationEntry other = (InvitationEntry) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.code, other.code) && Objects.equal(this.email, other.email)
				&& Objects.equal(this.groups, other.groups)
				&& Objects.equal(this.requestedTime, other.requestedTime)
				&& Objects.equal(this.expirationTime, other.expirationTime)
				&& Objects.equal(this.link, other.link);
	}

}
