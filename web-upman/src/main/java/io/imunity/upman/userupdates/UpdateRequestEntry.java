/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;

import pl.edu.icm.unity.engine.api.project.ProjectRequest.RequestOperation;

/***
 * Data object behind a row in {@link UpdateRequestsGrid}. Stores request
 * information
 * 
 * @author P.Piernik
 *
 */
class UpdateRequestEntry
{
	public final String id;
	public final RequestOperation operation;
	public final String name;
	public final String email;
	public final List<String> groupsDisplayedNames;
	public final Instant requestedTime;

	public UpdateRequestEntry(String id, RequestOperation operation, String email, String name,
			List<String> groupsDisplayedNames, Instant requestedTime)
	{
		this.id = id;
		this.operation = operation;
		this.name = name;
		this.email = email;
		this.groupsDisplayedNames = new ArrayList<>(groupsDisplayedNames);
		this.requestedTime = requestedTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(id, operation, name, email, requestedTime, groupsDisplayedNames);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final UpdateRequestEntry other = (UpdateRequestEntry) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.id, other.id) && Objects.equal(this.operation, other.operation)
				&& Objects.equal(this.email, other.email) && Objects.equal(this.name, other.name)
				&& Objects.equal(this.groupsDisplayedNames, other.groupsDisplayedNames)
				&& Objects.equal(this.requestedTime, other.requestedTime);

	}

}
