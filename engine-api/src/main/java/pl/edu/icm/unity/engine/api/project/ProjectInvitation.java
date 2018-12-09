/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.time.Instant;

import com.google.common.base.Objects;

import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

/**
 * Complete project invitation as stored in the system.
 * 
 * @author P.Piernik
 *
 */
public class ProjectInvitation extends ProjectInvitationParam
{
	public final String registrationCode;
	public final Instant lastSentTime;
	public final int numberOfSends;
	public final String link;

	public ProjectInvitation(String project, InvitationWithCode org, String link)
	{
		
		super(project, org.getContactAddress(), org.getAllowedGroups().get(0).getSelectedGroups(), org.getExpiration());
		this.registrationCode = org.getRegistrationCode();
		this.lastSentTime = org.getLastSentTime();
		this.numberOfSends = org.getNumberOfSends();
		this.link = link;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(super.hashCode(), registrationCode, lastSentTime, numberOfSends, link);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final ProjectInvitation other = (ProjectInvitation) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.registrationCode, other.registrationCode)
				&& Objects.equal(this.lastSentTime, other.lastSentTime)
				&& Objects.equal(this.numberOfSends, other.numberOfSends)
				&& Objects.equal(this.link, other.link);
	}
}
