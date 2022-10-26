/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.authn;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestCredentialPublicInformation.Builder.class)
public class RestCredentialPublicInformation
{
	public final String state;
	public final String stateDetail;
	public final String extraInformation;

	private RestCredentialPublicInformation(Builder builder)
	{
		this.state = builder.state;
		this.stateDetail = builder.stateDetail;
		this.extraInformation = builder.extraInformation;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(extraInformation, state, stateDetail);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RestCredentialPublicInformation other = (RestCredentialPublicInformation) obj;
		return Objects.equals(extraInformation, other.extraInformation) && Objects.equals(state, other.state)
				&& Objects.equals(stateDetail, other.stateDetail);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String state;
		private String stateDetail;
		private String extraInformation;

		private Builder()
		{
		}

		public Builder withState(String state)
		{
			this.state = state;
			return this;
		}

		public Builder withStateDetail(String stateDetail)
		{
			this.stateDetail = stateDetail;
			return this;
		}

		public Builder withExtraInformation(String extraInformation)
		{
			this.extraInformation = extraInformation;
			return this;
		}

		public RestCredentialPublicInformation build()
		{
			return new RestCredentialPublicInformation(this);
		}
	}

}
