/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;

import io.imunity.upman.common.FilterableEntry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestType;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;

/***
 * Data object behind a row in {@link UpdateRequestsGrid}. Stores request
 * information
 * 
 * @author P.Piernik
 *
 */
class UpdateRequestEntry implements FilterableEntry
{
	public final String id;
	public final RequestOperation operation;
	public final RequestType type;
	public final String name;
	public final VerifiableElementBase email;
	public final List<String> groupsDisplayedNames;
	public final Instant requestedTime;

	public UpdateRequestEntry(String id, RequestOperation operation, RequestType type, VerifiableElementBase email, String name,
			List<String> groupsDisplayedNames, Instant requestedTime)
	{
		this.id = id;
		this.operation = operation;
		this.type = type;
		this.name = name;
		this.email = email;
		this.groupsDisplayedNames = groupsDisplayedNames == null ? Collections.unmodifiableList(Collections.emptyList())
				: Collections.unmodifiableList(groupsDisplayedNames);
		this.requestedTime = requestedTime;
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(id, operation, type, name, email, requestedTime, groupsDisplayedNames);
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
				 && Objects.equal(this.type, other.type)
				&& Objects.equal(this.email, other.email) && Objects.equal(this.name, other.name)
				&& Objects.equal(this.groupsDisplayedNames, other.groupsDisplayedNames)
				&& Objects.equal(this.requestedTime, other.requestedTime);

	}

	@Override
	public boolean anyFieldContains(String searched, UnityMessageSource msg)
	{

		String textLower = searched.toLowerCase();

		if (operation != null && msg.getMessage("UpdateRequest." + operation.toString().toLowerCase())
				.toLowerCase().contains(textLower))
			return true;

		if (name != null && name.toLowerCase().toLowerCase().contains(textLower))
			return true;

		if (email != null && email.getValue().toLowerCase().contains(textLower))
			return true;

		if (requestedTime != null && TimeUtil.formatMediumInstant(requestedTime).toString().toLowerCase()
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
