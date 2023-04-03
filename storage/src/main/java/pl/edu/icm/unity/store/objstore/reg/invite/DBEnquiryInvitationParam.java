/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBEnquiryInvitationParam.Builder.class)
public class DBEnquiryInvitationParam extends DBInvitationParam
{
	public static final String type = "ENQUIRY";

	
	public final Long entity;
	@JsonUnwrapped
	public final DBFormPrefill formPrefill;

	private DBEnquiryInvitationParam(Builder builder)
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
		DBEnquiryInvitationParam other = (DBEnquiryInvitationParam) obj;
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
		private DBFormPrefill formPrefill;

		private Builder()
		{
			super(type);
		}

		public Builder withEntity(Long entity)
		{
			this.entity = entity;
			return this;
		}

		public Builder withFormPrefill(DBFormPrefill formPrefill)
		{
			this.formPrefill = formPrefill;
			return this;
		}

		public DBEnquiryInvitationParam build()
		{
			return new DBEnquiryInvitationParam(this);
		}
	}
}
