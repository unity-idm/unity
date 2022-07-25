/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.invitations;

import com.google.common.base.Objects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static pl.edu.icm.unity.engine.api.utils.TimeUtil.formatStandardInstant;

class InvitationModel
{
	public final String email;
	public final List<String> groupsDisplayedNames;
	public final Instant requestedTime;
	public final Instant expirationTime;
	public final String link;
	public final String code;

	public InvitationModel(String code, String email, List<String> groupsDisplayedNames, Instant requestedTime,
	                       Instant expirationTime, String link)
	{
		this.code = code;
		this.email = email;
		this.groupsDisplayedNames = new ArrayList<>();
		if (groupsDisplayedNames != null)
		{
			this.groupsDisplayedNames.addAll(groupsDisplayedNames);
		}
		this.requestedTime = requestedTime;
		this.expirationTime = expirationTime;
		this.link = link;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(code, email, groupsDisplayedNames, requestedTime, expirationTime, link);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final InvitationModel other = (InvitationModel) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.code, other.code) && Objects.equal(this.email, other.email)
				&& Objects.equal(this.groupsDisplayedNames, other.groupsDisplayedNames)
				&& Objects.equal(this.requestedTime, other.requestedTime)
				&& Objects.equal(this.expirationTime, other.expirationTime)
				&& Objects.equal(this.link, other.link);
	}

	public boolean anyFieldContains(String value)
	{
		String lowerCaseValue = value.toLowerCase();
		return value.isEmpty()
				|| email.toLowerCase().contains(lowerCaseValue)
				|| (requestedTime != null && formatStandardInstant(requestedTime).toLowerCase().contains(lowerCaseValue))
				|| (expirationTime != null && formatStandardInstant(expirationTime).toLowerCase().contains(lowerCaseValue))
				|| groupsDisplayedNames.stream().anyMatch(grp -> grp.toLowerCase().contains(lowerCaseValue));
	}
}
