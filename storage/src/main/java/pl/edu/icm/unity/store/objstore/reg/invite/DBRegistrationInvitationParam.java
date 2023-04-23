/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBRegistrationInvitationParam.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBRegistrationInvitationParam extends DBInvitationParam
{
	public static final String type = "REGISTRATION";
	
	public final DBExpectedIdentity expectedIdentity;
	@JsonUnwrapped
	public final DBFormPrefill formPrefill;

	private DBRegistrationInvitationParam(Builder builder)
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
		DBRegistrationInvitationParam other = (DBRegistrationInvitationParam) obj;
		return Objects.equals(expectedIdentity, other.expectedIdentity)
				&& Objects.equals(formPrefill, other.formPrefill);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder extends RestInvitationParamBuilder<Builder>
	{

		private DBExpectedIdentity expectedIdentity;
		@JsonUnwrapped
		private DBFormPrefill formPrefill;

		private Builder()
		{
			super(type);
		}

		public Builder withExpectedIdentity(DBExpectedIdentity expectedIdentity)
		{
			this.expectedIdentity = expectedIdentity;
			return this;
		}

		public Builder withFormPrefill(DBFormPrefill formPrefill)
		{
			this.formPrefill = formPrefill;
			return this;
		}

		public DBRegistrationInvitationParam build()
		{
			return new DBRegistrationInvitationParam(this);
		}
	}
}
