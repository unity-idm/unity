/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.authn.RestCredentialInfo;

@JsonDeserialize(builder = RestEntity.Builder.class)
public class RestEntity
{
	public final RestEntityInformation entityInformation;
	public final List<RestIdentity> identities;
	public final RestCredentialInfo credentialInfo;

	private RestEntity(Builder builder)
	{
		this.entityInformation = builder.entityInformation;
		this.identities = List.copyOf(builder.identities);
		this.credentialInfo = builder.credentialInfo;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(credentialInfo, entityInformation, identities);
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
		RestEntity other = (RestEntity) obj;
		return Objects.equals(credentialInfo, other.credentialInfo)
				&& Objects.equals(entityInformation, other.entityInformation)
				&& Objects.equals(identities, other.identities);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private RestEntityInformation entityInformation;
		private List<RestIdentity> identities = Collections.emptyList();
		private RestCredentialInfo credentialInfo;

		private Builder()
		{
		}

		public Builder withEntityInformation(RestEntityInformation entityInformation)
		{
			this.entityInformation = entityInformation;
			return this;
		}

		public Builder withIdentities(List<RestIdentity> identities)
		{
			this.identities = identities;
			return this;
		}

		public Builder withCredentialInfo(RestCredentialInfo credentialInfo)
		{
			this.credentialInfo = credentialInfo;
			return this;
		}

		public RestEntity build()
		{
			return new RestEntity(this);
		}
	}

}
