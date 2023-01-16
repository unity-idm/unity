/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.invite;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestRegistrationInvitationParam.Builder.class)
public class RestRegistrationInvitationParam extends RestInvitationParam
{
	public static final String type = "REGISTRATION";
	
	public final RestExpectedIdentity expectedIdentity;
	@JsonUnwrapped
	public final RestFormPrefill formPrefill;

	private RestRegistrationInvitationParam(Builder builder)
	{
		super(builder);
		this.expectedIdentity = builder.expectedIdentity;
		this.formPrefill = builder.formPrefill;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(expectedIdentity, formPrefill);
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
		RestRegistrationInvitationParam other = (RestRegistrationInvitationParam) obj;
		return Objects.equals(expectedIdentity, other.expectedIdentity)
				&& Objects.equals(formPrefill, other.formPrefill);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestInvitationParamBuilder<Builder>
	{

		private RestExpectedIdentity expectedIdentity;
		@JsonUnwrapped
		private RestFormPrefill formPrefill;

		private Builder()
		{
			super(type);
		}

		public Builder withExpectedIdentity(RestExpectedIdentity expectedIdentity)
		{
			this.expectedIdentity = expectedIdentity;
			return this;
		}

		public Builder withFormPrefill(RestFormPrefill formPrefill)
		{
			this.formPrefill = formPrefill;
			return this;
		}

		public RestRegistrationInvitationParam build()
		{
			return new RestRegistrationInvitationParam(this);
		}
	}
}
