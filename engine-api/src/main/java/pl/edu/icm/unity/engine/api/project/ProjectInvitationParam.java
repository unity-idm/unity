/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.time.Instant;
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
	private String project;
	private String contactAddress;
	private List<String> groupPaths;
	private Instant expiration;

	// for binder
	public ProjectInvitationParam()
	{

	}

	public String getProject()
	{
		return project;
	}

	public void setProject(String project)
	{
		this.project = project;
	}

	public List<String> getGroupPaths()
	{
		return groupPaths;
	}

	public void setGroupPaths(List<String> groupPaths)
	{
		this.groupPaths = groupPaths;
	}

	public Instant getExpiration()
	{
		return expiration;
	}

	public void setExpiration(Instant expiration)
	{
		this.expiration = expiration;
	}

	public String getContactAddress()
	{
		return contactAddress;
	}

	public void setContactAddress(String contactAddress)
	{
		this.contactAddress = contactAddress;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(project, groupPaths, expiration, contactAddress);
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

		return Objects.equal(this.project, other.project) && Objects.equal(this.groupPaths, other.groupPaths)
				&& Objects.equal(this.expiration, other.expiration)
				&& Objects.equal(this.contactAddress, contactAddress);
	}
}
