/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Objects;

import pl.edu.icm.unity.types.basic.VerifiableElementBase;

/**
 * Holds information about project request
 * 
 * @author P.Piernik
 *
 */
public class ProjectRequest extends ProjectRequestParam
{
	public final String name;
	public final VerifiableElementBase email;
	public final List<String> groups;
	public final Instant requestedTime;

	public ProjectRequest(String id, RequestOperation operation, RequestType type, String project, String name,
			VerifiableElementBase email, Optional<List<String>> groups, Instant requestedTime)
	{
		super(project, id, operation, type);
		this.name = name;
		this.email = email;
		this.groups = !groups.isPresent() ? Collections.unmodifiableList(Collections.emptyList())
				: Collections.unmodifiableList(groups.get());
		this.requestedTime = requestedTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(id, operation, type, project, name, email, groups, requestedTime);
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

		return Objects.equal(this.email, other.email) && Objects.equal(this.name, other.name)
				&& Objects.equal(this.groups, other.groups)
				&& Objects.equal(this.requestedTime, other.requestedTime);
	}
}
