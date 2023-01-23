/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestRegistrationRequestState.Builder.class)
public class RestRegistrationRequestState extends RestUserRequestState<RestRegistrationRequest>
{
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("CreatedEntityId")
	public final Long createdEntityId;

	private RestRegistrationRequestState(Builder builder)
	{
		super(builder);
		this.createdEntityId = builder.createdEntityId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(createdEntityId);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestRegistrationRequestState other = (RestRegistrationRequestState) obj;
		return Objects.equals(createdEntityId, other.createdEntityId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestUserRequestStateBuilder<RestRegistrationRequest, Builder>
	{
		@JsonProperty("CreatedEntityId")
		private Long createdEntityId;

		public Builder withCreatedEntityId(Long createdEntityId)
		{
			this.createdEntityId = createdEntityId;
			return this;
		}

		public RestRegistrationRequestState build()
		{
			return new RestRegistrationRequestState(this);
		}
	}
}
