/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;

/**
 * Holds information about project request
 * @author P.Piernik
 *
 */
public class ProjectRequest
{
	public static enum RequestOperation
	{
		Update, SelfSignUp
	};

	public final String id;
	public final RequestOperation operation;
	public final String project;
	public final String name;
	public final String email;
	public final List<String> groups;
	public final Instant requestedTime;

	public ProjectRequest(String id, RequestOperation operation, String project, String name, String email,
			List<String> groups, Instant requestedTime)
	{
		this.id = id;
		this.operation = operation;
		this.project = project;
		this.name = name;
		this.email = email;
		this.groups = new ArrayList<>();
		if (groups != null)
		{
			this.groups.addAll(groups);
		}
		this.requestedTime = requestedTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(id, operation, project, name, email, groups, requestedTime);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final ProjectRequest other = (ProjectRequest) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.id, other.id) && Objects.equal(this.operation, other.operation)
				&& Objects.equal(this.project, other.project) && Objects.equal(this.email, other.email)
				&& Objects.equal(this.name, other.name) && Objects.equal(this.groups, other.groups)
				&& Objects.equal(this.requestedTime, other.requestedTime);
	}
}
