/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBComboInvitationParam.Builder.class)
public class DBComboInvitationParam extends DBInvitationParam
{
	public static final String type = "COMBO";
	
	public final DBFormPrefill registrationFormPrefill;
	public final DBFormPrefill enquiryFormPrefill;

	private DBComboInvitationParam(Builder builder)
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
		DBComboInvitationParam other = (DBComboInvitationParam) obj;
		return Objects.equals(enquiryFormPrefill, other.enquiryFormPrefill)
				&& Objects.equals(registrationFormPrefill, other.registrationFormPrefill);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestInvitationParamBuilder<Builder>
	{
		private DBFormPrefill registrationFormPrefill;
		private DBFormPrefill enquiryFormPrefill;

		private Builder()
		{
			super(type);
		}

		public Builder withRegistrationFormPrefill(DBFormPrefill registrationFormPrefill)
		{
			this.registrationFormPrefill = registrationFormPrefill;
			return this;
		}

		public Builder withEnquiryFormPrefill(DBFormPrefill enquiryFormPrefill)
		{
			this.enquiryFormPrefill = enquiryFormPrefill;
			return this;
		}

		public DBComboInvitationParam build()
		{
			return new DBComboInvitationParam(this);
		}
	}
}
