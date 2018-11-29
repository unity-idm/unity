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
	private String registrationCode;
	private Instant lastSentTime;
	private int numberOfSends;
	private String link;

	public ProjectInvitation(String project, InvitationWithCode org)
	{
		this.registrationCode = org.getRegistrationCode();
		this.lastSentTime = org.getLastSentTime();
		this.numberOfSends = org.getNumberOfSends();
		setContactAddress(org.getContactAddress());
		setExpiration(org.getExpiration());
		// TODO
		// setGroupPaths(groupPaths);
		setProject(project);
	}

	public String getRegistrationCode()
	{
		return registrationCode;
	}

	public Instant getLastSentTime()
	{
		return lastSentTime;
	}

	public int getNumberOfSends()
	{
		return numberOfSends;
	}

	public String getLink()
	{
		return link;
	}

	public void setLink(String link)
	{
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
