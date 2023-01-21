/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.userimport;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.registration.RestAdminComment;

@JsonDeserialize(builder = RestAdminComment.Builder.class)
public class RestImportResult
{
	public final String importerKey;
	public final String status;
	public final String authenticationResult;

	private RestImportResult(Builder builder)
	{
		this.importerKey = builder.importerKey;
		this.status = builder.status;
		this.authenticationResult = builder.authenticationResult;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(authenticationResult, importerKey, status);
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
		RestImportResult other = (RestImportResult) obj;
		return Objects.equals(authenticationResult, other.authenticationResult)
				&& Objects.equals(importerKey, other.importerKey) && Objects.equals(status, other.status);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String importerKey;
		private String status;
		private String authenticationResult;

		private Builder()
		{
		}

		public Builder withImporterKey(String importerKey)
		{
			this.importerKey = importerKey;
			return this;
		}

		public Builder withStatus(String status)
		{
			this.status = status;
			return this;
		}

		public Builder withAuthenticationResult(String authenticationResult)
		{
			this.authenticationResult = authenticationResult;
			return this;
		}

		public RestImportResult build()
		{
			return new RestImportResult(this);
		}
	}

}
