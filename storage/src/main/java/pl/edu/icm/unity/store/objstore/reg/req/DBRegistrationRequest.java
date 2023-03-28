/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.req;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.objstore.reg.common.DBBaseRegistrationInput;

@JsonDeserialize(builder = DBRegistrationRequest.Builder.class)
public class DBRegistrationRequest extends DBBaseRegistrationInput
{
	private DBRegistrationRequest(Builder builder)
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

	public static final class Builder extends RestBaseRegistrationInputBuilder<Builder>
	{
		public Builder()
		{
		}

		public DBRegistrationRequest build()
		{
			return new DBRegistrationRequest(this);
		}
	}
}
