/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

/**
 * Base data of project invitation parameter
 * 
 * @author P.Piernik
 *
 */
public class ProjectInvitationParam
{
	public final String project;
	public final String contactAddress;
	public final List<String> groups;
	public final boolean allowModifyGroups;
	public final Instant expiration;

	public ProjectInvitationParam(String project, String contactAddress, List<String> groups,
			boolean allowModifyGroups, Instant expiration)
	{
		this.project = project;
		this.contactAddress = contactAddress;
		this.groups = Collections.unmodifiableList(groups != null ? groups : Collections.emptyList());
		this.expiration = expiration;
		this.allowModifyGroups = allowModifyGroups;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(project, groups, expiration, contactAddress, allowModifyGroups);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final ProjectInvitationParam other = (ProjectInvitationParam) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.project, other.project)
				&& Objects.equal(this.groups, other.groups)
				&& Objects.equal(this.expiration, other.expiration)
				&& Objects.equal(this.contactAddress, contactAddress);
	}
}
