/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestRegistrationRequest.Builder.class)
public class RestRegistrationRequest extends RestBaseRegistrationInput
{
	private RestRegistrationRequest(Builder builder)
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

		public RestRegistrationRequest build()
		{
			return new RestRegistrationRequest(this);
		}
	}
}
