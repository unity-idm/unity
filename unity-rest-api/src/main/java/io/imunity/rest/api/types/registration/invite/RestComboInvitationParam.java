/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.invite;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestComboInvitationParam.Builder.class)
public class RestComboInvitationParam extends RestInvitationParam
{
	public static final String type = "COMBO";
	
	public final RestFormPrefill registrationFormPrefill;
	public final RestFormPrefill enquiryFormPrefill;

	private RestComboInvitationParam(Builder builder)
	{
		super(builder);
		this.registrationFormPrefill = builder.registrationFormPrefill;
		this.enquiryFormPrefill = builder.enquiryFormPrefill;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(enquiryFormPrefill, registrationFormPrefill);
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
		RestComboInvitationParam other = (RestComboInvitationParam) obj;
		return Objects.equals(enquiryFormPrefill, other.enquiryFormPrefill)
				&& Objects.equals(registrationFormPrefill, other.registrationFormPrefill);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestInvitationParamBuilder<Builder>
	{
		private RestFormPrefill registrationFormPrefill;
		private RestFormPrefill enquiryFormPrefill;

		private Builder()
		{
			super(type);
		}

		public Builder withRegistrationFormPrefill(RestFormPrefill registrationFormPrefill)
		{
			this.registrationFormPrefill = registrationFormPrefill;
			return this;
		}

		public Builder withEnquiryFormPrefill(RestFormPrefill enquiryFormPrefill)
		{
			this.enquiryFormPrefill = enquiryFormPrefill;
			return this;
		}

		public RestComboInvitationParam build()
		{
			return new RestComboInvitationParam(this);
		}
	}
}
