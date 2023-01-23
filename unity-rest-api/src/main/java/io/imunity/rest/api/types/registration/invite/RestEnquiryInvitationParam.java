/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.invite;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestEnquiryInvitationParam.Builder.class)
public class RestEnquiryInvitationParam extends RestInvitationParam
{
	public static final String type = "ENQUIRY";

	
	public final Long entity;
	@JsonUnwrapped
	public final RestFormPrefill formPrefill;

	private RestEnquiryInvitationParam(Builder builder)
	{
		super(builder);
		this.entity = builder.entity;
		this.formPrefill = builder.formPrefill;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(entity, formPrefill);
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
		RestEnquiryInvitationParam other = (RestEnquiryInvitationParam) obj;
		return Objects.equals(entity, other.entity)
				&& Objects.equals(formPrefill, other.formPrefill);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestInvitationParamBuilder<Builder>
	{

		private Long entity;
		@JsonUnwrapped
		private RestFormPrefill formPrefill;

		private Builder()
		{
			super(type);
		}

		public Builder withEntity(Long entity)
		{
			this.entity = entity;
			return this;
		}

		public Builder withFormPrefill(RestFormPrefill formPrefill)
		{
			this.formPrefill = formPrefill;
			return this;
		}

		public RestEnquiryInvitationParam build()
		{
			return new RestEnquiryInvitationParam(this);
		}
	}
}
