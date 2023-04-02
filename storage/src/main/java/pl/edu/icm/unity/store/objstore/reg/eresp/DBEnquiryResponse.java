/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.eresp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBBaseRegistrationInput;

@JsonDeserialize(builder = DBEnquiryResponse.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBEnquiryResponse extends DBBaseRegistrationInput
{
	private DBEnquiryResponse(Builder builder)
	{
		super(builder);
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
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
		return true;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static final class Builder extends DBBaseRegistrationInputBuilder<Builder>
	{
		public Builder()
		{
		}

		public DBEnquiryResponse build()
		{
			return new DBEnquiryResponse(this);
		}
	}
}
