/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.authn;

import java.util.Map;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;

@JsonDeserialize(builder = RestCredentialInfo.Builder.class)
public class RestCredentialInfo
{
	public final String credentialRequirementId;
	public final Map<String, RestCredentialPublicInformation> credentialsState;

	private RestCredentialInfo(Builder builder)
	{
		this.credentialRequirementId = builder.credentialRequirementId;
		this.credentialsState = Map.copyOf(builder.credentialsState);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(credentialRequirementId, credentialsState);
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
		RestCredentialInfo other = (RestCredentialInfo) obj;
		return Objects.equals(credentialRequirementId, other.credentialRequirementId)
				&& Objects.equals(credentialsState, other.credentialsState);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String credentialRequirementId;
		private Map<String, RestCredentialPublicInformation> credentialsState = Collections.emptyMap();

		private Builder()
		{
		}

		public Builder withCredentialRequirementId(String credentialRequirementId)
		{
			this.credentialRequirementId = credentialRequirementId;
			return this;
		}

		public Builder withCredentialsState(Map<String, RestCredentialPublicInformation> credentialsState)
		{
			this.credentialsState = credentialsState;
			return this;
		}

		public RestCredentialInfo build()
		{
			return new RestCredentialInfo(this);
		}
	}

}
