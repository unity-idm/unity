/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBIdentityRegistrationParam.Builder.class)
public class DBIdentityRegistrationParam extends DBRegistrationParam
{
	public final String identityType;
	public final String confirmationMode;
	public final DBurlQueryPrefillConfig urlQueryPrefill;
	public final boolean optional;

	private DBIdentityRegistrationParam(Builder builder)
	{
		super(builder);

		this.identityType = builder.identityType;
		this.confirmationMode = builder.confirmationMode;
		this.urlQueryPrefill = builder.urlQueryPrefill;
		this.optional = builder.optional;
	}

	
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(confirmationMode, identityType, optional, urlQueryPrefill);
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
		DBIdentityRegistrationParam other = (DBIdentityRegistrationParam) obj;
		return Objects.equals(confirmationMode, other.confirmationMode)
				&& Objects.equals(identityType, other.identityType) && optional == other.optional
				&& Objects.equals(urlQueryPrefill, other.urlQueryPrefill);
	}



	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder extends RestRegistrationParamBuilder<Builder>
	{

		private String identityType;
		private String confirmationMode;
		private DBurlQueryPrefillConfig urlQueryPrefill;
		private boolean optional;

		private Builder()
		{
		}

		public Builder withIdentityType(String identityType)
		{
			this.identityType = identityType;
			return this;
		}

		public Builder withConfirmationMode(String confirmationMode)
		{
			this.confirmationMode = confirmationMode;
			return this;
		}

		public Builder withUrlQueryPrefill(DBurlQueryPrefillConfig urlQueryPrefill)
		{
			this.urlQueryPrefill = urlQueryPrefill;
			return this;
		}

		public Builder withOptional(boolean optional)
		{
			this.optional = optional;
			return this;
		}

		public DBIdentityRegistrationParam build()
		{
			return new DBIdentityRegistrationParam(this);
		}
	}

}
