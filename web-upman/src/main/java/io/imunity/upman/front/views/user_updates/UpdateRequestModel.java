/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.user_updates;

import com.google.common.base.Objects;
import io.imunity.upman.front.model.EmailModel;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation;
import pl.edu.icm.unity.engine.api.registration.RequestType;

import java.time.Instant;
import java.util.List;

import static pl.edu.icm.unity.engine.api.utils.TimeUtil.formatStandardInstant;

class UpdateRequestModel
{
	public final String id;
	public final RequestOperation operation;
	public final RequestType type;
	public final String name;
	public final EmailModel email;
	public final List<String> groupsDisplayedNames;
	public final Instant requestedTime;

	private UpdateRequestModel(String id, RequestOperation operation, RequestType type, EmailModel email, String name,
	                          List<String> groupsDisplayedNames, Instant requestedTime)
	{
		this.id = id;
		this.operation = operation;
		this.type = type;
		this.name = name;
		this.email = email;
		this.groupsDisplayedNames = groupsDisplayedNames;
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
		final UpdateRequestModel other = (UpdateRequestModel) obj;

		if (!super.equals(obj))
			return false;

		return Objects.equal(this.id, other.id) && Objects.equal(this.operation, other.operation)
				&& Objects.equal(this.type, other.type)
				&& Objects.equal(this.email, other.email) && Objects.equal(this.name, other.name)
				&& Objects.equal(this.groupsDisplayedNames, other.groupsDisplayedNames)
				&& Objects.equal(this.requestedTime, other.requestedTime);
	}

	public boolean anyFieldContains(String value)
	{
		String lowerCaseValue = value.toLowerCase();
		return value.isEmpty()
				|| (operation != null && operation.name().toLowerCase().contains(value.replace(" ","")))
				|| (name != null && name.toLowerCase().contains(lowerCaseValue))
				|| (email.value != null && email.value.toLowerCase().contains(lowerCaseValue))
				|| (requestedTime != null && formatStandardInstant(requestedTime).toLowerCase().contains(lowerCaseValue))
				|| (groupsDisplayedNames != null && groupsDisplayedNames.stream().anyMatch(grp -> grp.toLowerCase().contains(lowerCaseValue)));
	}

	public static UpdateRequestModelBuilder builder()
	{
		return new UpdateRequestModelBuilder();
	}

	public static final class UpdateRequestModelBuilder
	{
		private String id;
		private RequestOperation operation;
		private RequestType type;
		private String name;
		private EmailModel email;
		private List<String> groupsDisplayedNames;
		private Instant requestedTime;

		private UpdateRequestModelBuilder()
		{
		}

		public UpdateRequestModelBuilder id(String id)
		{
			this.id = id;
			return this;
		}

		public UpdateRequestModelBuilder operation(RequestOperation operation)
		{
			this.operation = operation;
			return this;
		}

		public UpdateRequestModelBuilder type(RequestType type)
		{
			this.type = type;
			return this;
		}

		public UpdateRequestModelBuilder name(String name)
		{
			this.name = name;
			return this;
		}

		public UpdateRequestModelBuilder email(VerifiableElementBase email)
		{
			this.email = EmailModel.of(email);
			return this;
		}

		public UpdateRequestModelBuilder groupsDisplayedNames(List<String> groupsDisplayedNames)
		{
			this.groupsDisplayedNames = groupsDisplayedNames;
			return this;
		}

		public UpdateRequestModelBuilder requestedTime(Instant requestedTime)
		{
			this.requestedTime = requestedTime;
			return this;
		}

		public UpdateRequestModel build()
		{
			return new UpdateRequestModel(id, operation, type, email, name, groupsDisplayedNames, requestedTime);
		}
	}
}
