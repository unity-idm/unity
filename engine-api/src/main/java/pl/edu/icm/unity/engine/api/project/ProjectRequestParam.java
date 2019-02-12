/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import com.google.common.base.Objects;

/**
 * Holds base information about project request
 * 
 * @author P.Piernik
 *
 **/
public class ProjectRequestParam
{
	public static enum RequestOperation
	{
		Update, SignUp
	};

	public static enum RequestType
	{
		Registration, Enquiry
	};

	public final String id;
	public final String project;
	public final RequestOperation operation;
	public final RequestType type;

	public ProjectRequestParam(String project, String id, RequestOperation operation, RequestType type)
	{
		this.id = id;
		this.operation = operation;
		this.type = type;
		this.project = project;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(id, operation, type, project);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final ProjectRequestParam other = (ProjectRequestParam) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.id, other.id) && Objects.equal(this.operation, other.operation)
				&& Objects.equal(this.type, other.type) && Objects.equal(this.project, other.project);
	}

}
