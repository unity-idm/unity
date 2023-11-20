/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEnquiryResponseState.Builder.class)
public class RestEnquiryResponseState extends RestUserRequestState<RestEnquiryResponse>
{
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("EntityId")
	public final Long entityId;

	private RestEnquiryResponseState(Builder builder)
	{
		super(builder);
		this.entityId = builder.entityId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(entityId);
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
		RestEnquiryResponseState other = (RestEnquiryResponseState) obj;
		return Objects.equals(entityId, other.entityId);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestUserRequestStateBuilder<RestEnquiryResponse, Builder>
	{
		@JsonProperty("EntityId")
		private Long entityId;

		public Builder withEntityId(Long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public RestEnquiryResponseState build()
		{
			return new RestEnquiryResponseState(this);
		}
	}
}
