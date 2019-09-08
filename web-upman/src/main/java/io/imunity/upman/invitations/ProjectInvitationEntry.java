/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

/***
 * Data object behind a row in {@link ProjectInvitationsGrid}. Stores invitation
 * information
 * 
 * @author P.Piernik
 *
 */
class ProjectInvitationEntry implements FilterableEntry
{
	public final String email;
	public final List<String> groupsDisplayedNames;
	public final Instant requestedTime;
	public final Instant expirationTime;
	public final String link;
	public final String code;

	public ProjectInvitationEntry(String code, String email, List<String> groupsDisplayedNames, Instant requestedTime,
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
		final ProjectInvitationEntry other = (ProjectInvitationEntry) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.code, other.code) && Objects.equal(this.email, other.email)
				&& Objects.equal(this.groupsDisplayedNames, other.groupsDisplayedNames)
				&& Objects.equal(this.requestedTime, other.requestedTime)
				&& Objects.equal(this.expirationTime, other.expirationTime)
				&& Objects.equal(this.link, other.link);
	}

	@Override
	public boolean anyFieldContains(String searched, UnityMessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (email != null && email.toLowerCase().contains(textLower))
			return true;

		if (requestedTime != null && TimeUtil.formatStandardInstant(requestedTime).toString().toLowerCase()
				.contains(textLower))
			return true;

		if (expirationTime != null && TimeUtil.formatStandardInstant(expirationTime).toString().toLowerCase()
				.contains(textLower))
			return true;

		for (String group : groupsDisplayedNames)
		{
			if (group != null && group.toLowerCase().contains(textLower))
				return true;
		}

		return false;
	}
}
