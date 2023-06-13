/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.invite.FormPrefill;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;

/**
 * Holds information about project invitation.
 * 
 * @author P.Piernik
 *
 */
public class ProjectInvitation extends ProjectInvitationParam
{
	public static final long DEFAULT_TTL_DAYS = 3;

	public final String registrationCode;
	public final Instant lastSentTime;
	public final int numberOfSends;
	public final String link;

	public ProjectInvitation(String project, BaseForm form, InvitationWithCode org, String link) throws EngineException
	{
		super(project, org.getInvitation().getContactAddress(), getGroups(org, form),
				org.getInvitation().getPrefillForForm(form).getAllowedGroups() != null
						&& !org.getInvitation().getPrefillForForm(form).getAllowedGroups().isEmpty(),
				org.getInvitation().getExpiration());
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

	private static List<String> getGroups(InvitationWithCode org, BaseForm form) throws EngineException
	{
		FormPrefill invParam = org.getInvitation().getPrefillForForm(form);
		
		if ((invParam.getAllowedGroups() == null || invParam.getAllowedGroups().isEmpty())
				&& (invParam.getGroupSelections() == null || invParam.getGroupSelections().isEmpty()))
		{
			return Collections.emptyList();
		}

		return !invParam.getAllowedGroups().isEmpty() ? invParam.getAllowedGroups().get(0).getSelectedGroups()
				: invParam.getGroupSelections().get(0).getEntry().getSelectedGroups();
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
