/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.registration.layout.RestFormLayout;
@JsonDeserialize(builder = RestRegistrationFormLayouts.Builder.class)
public class RestRegistrationFormLayouts
{
	public final RestFormLayout primaryLayout;
	public final RestFormLayout secondaryLayout;
	public final boolean localSignupEmbeddedAsButton;

	private RestRegistrationFormLayouts(Builder builder)
	{
		this.primaryLayout = builder.primaryLayout;
		this.secondaryLayout = builder.secondaryLayout;
		this.localSignupEmbeddedAsButton = builder.localSignupEmbeddedAsButton;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(localSignupEmbeddedAsButton, primaryLayout, secondaryLayout);
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
		RestRegistrationFormLayouts other = (RestRegistrationFormLayouts) obj;
		return localSignupEmbeddedAsButton == other.localSignupEmbeddedAsButton
				&& Objects.equals(primaryLayout, other.primaryLayout)
				&& Objects.equals(secondaryLayout, other.secondaryLayout);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private RestFormLayout primaryLayout;
		private RestFormLayout secondaryLayout;
		private boolean localSignupEmbeddedAsButton;

		private Builder()
		{
		}

		public Builder withPrimaryLayout(RestFormLayout primaryLayout)
		{
			this.primaryLayout = primaryLayout;
			return this;
		}

		public Builder withSecondaryLayout(RestFormLayout secondaryLayout)
		{
			this.secondaryLayout = secondaryLayout;
			return this;
		}

		public Builder withLocalSignupEmbeddedAsButton(boolean localSignupEmbeddedAsButton)
		{
			this.localSignupEmbeddedAsButton = localSignupEmbeddedAsButton;
			return this;
		}

		public RestRegistrationFormLayouts build()
		{
			return new RestRegistrationFormLayouts(this);
		}
	}

}
