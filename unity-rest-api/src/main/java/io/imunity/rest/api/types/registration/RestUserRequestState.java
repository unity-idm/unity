/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public abstract class RestUserRequestState<T extends RestBaseRegistrationInput>
{
	@JsonProperty("RequestId")
	public final String requestId;
	@JsonProperty("Timestamp")
	public final Date timestamp;
	@JsonUnwrapped
	public final T request;
	@JsonProperty("Context")
	public final RestRegistrationContext registrationContext;
	@JsonProperty("AdminComments")
	public final List<RestAdminComment> adminComments;
	@JsonProperty("Status")
	public final String status;

	protected RestUserRequestState(RestUserRequestStateBuilder<T, ?> builder)
	{
		this.requestId = builder.requestId;
		this.timestamp = builder.timestamp;
		this.request = builder.request;
		this.registrationContext = builder.registrationContext;
		this.adminComments = Optional.ofNullable(builder.adminComments)
				.map(ArrayList::new)
				.map(Collections::unmodifiableList)
				.orElse(null);
		this.status = builder.status;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(adminComments, registrationContext, request, requestId, status, timestamp);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestUserRequestState<?> other = (RestUserRequestState<?>) obj;
		return Objects.equals(adminComments, other.adminComments)
				&& Objects.equals(registrationContext, other.registrationContext)
				&& Objects.equals(request, other.request) && Objects.equals(requestId, other.requestId)
				&& Objects.equals(status, other.status) && Objects.equals(timestamp, other.timestamp);
	}

	protected static class RestUserRequestStateBuilder<T extends RestBaseRegistrationInput, S extends RestUserRequestStateBuilder<?, ?>>
	{
		@JsonProperty("RequestId")
		private String requestId;
		@JsonProperty("Timestamp")
		private Date timestamp;
		@JsonUnwrapped
		private T request;
		@JsonProperty("Context")
		private RestRegistrationContext registrationContext;
		@JsonProperty("AdminComments")
		private List<RestAdminComment> adminComments = Collections.emptyList();
		@JsonProperty("Status")
		private String status;

		protected RestUserRequestStateBuilder()
		{
		}

		@SuppressWarnings("unchecked")
		public S withRequestId(String requestId)
		{
			this.requestId = requestId;
			return (S) this;
		}

		@SuppressWarnings("unchecked")
		public S withTimestamp(Date timestamp)
		{
			this.timestamp = timestamp;
			return (S) this;
		}

		@SuppressWarnings("unchecked")
		public S withRequest(T request)
		{
			this.request = request;
			return (S) this;
		}

		@SuppressWarnings("unchecked")
		public S withRegistrationContext(RestRegistrationContext registrationContext)
		{
			this.registrationContext = registrationContext;
			return (S) this;
		}

		@SuppressWarnings("unchecked")
		public S withAdminComments(List<RestAdminComment> adminComments)
		{
			this.adminComments = Optional.ofNullable(adminComments)
					.map(ArrayList::new)
					.map(Collections::unmodifiableList)
					.orElse(null);
			return (S) this;
		}

		@SuppressWarnings("unchecked")
		public S withStatus(String status)
		{
			this.status = status;
			return (S) this;
		}
	}

}
