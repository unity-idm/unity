/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz.externalScript;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ExternalAuthorizationScriptResponse.Builder.class)
public record ExternalAuthorizationScriptResponse(
		Status status,
		List<ExternalAuthzClaim> claims)
{
	public enum Status
	{
		DENY, PROCEED
	}

	public ExternalAuthorizationScriptResponse
	{
		claims = claims == null ? null : List.copyOf(claims);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Status status;
		private List<ExternalAuthzClaim> claims = Collections.emptyList();

		public Builder()
		{
		}

		public Builder withStatus(Status status)
		{
			this.status = status;
			return this;
		}

		public Builder withClaims(List<ExternalAuthzClaim> claims)
		{
			this.claims = claims;
			return this;
		}

		public ExternalAuthorizationScriptResponse build()
		{
			return new ExternalAuthorizationScriptResponse(status, Optional.ofNullable(claims)
					.map(List::copyOf)
					.orElse(null));
		}
	}

}
