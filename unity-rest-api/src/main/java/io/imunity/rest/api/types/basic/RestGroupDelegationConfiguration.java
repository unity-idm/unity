/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroupDelegationConfiguration.Builder.class)
public class RestGroupDelegationConfiguration
{
	public final boolean enabled;
	public final String logoUrl;
	public final String registrationForm;
	public final String signupEnquiryForm;
	public final String membershipUpdateEnquiryForm;
	public final List<String> attributes;
	public final boolean enableSubprojects;

	private RestGroupDelegationConfiguration(Builder builder)
	{
		this.enabled = builder.enabled;
		this.logoUrl = builder.logoUrl;
		this.registrationForm = builder.registrationForm;
		this.signupEnquiryForm = builder.signupEnquiryForm;
		this.membershipUpdateEnquiryForm = builder.membershipUpdateEnquiryForm;
		this.attributes = Optional.ofNullable(builder.attributes)
				.map(List::copyOf)
				.orElse(null);
		this.enableSubprojects = builder.enableSubprojects;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, enableSubprojects, enabled, logoUrl, membershipUpdateEnquiryForm,
				registrationForm, signupEnquiryForm);
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
		RestGroupDelegationConfiguration other = (RestGroupDelegationConfiguration) obj;
		return Objects.equals(attributes, other.attributes) && enableSubprojects == other.enableSubprojects
				&& enabled == other.enabled && Objects.equals(logoUrl, other.logoUrl)
				&& Objects.equals(membershipUpdateEnquiryForm, other.membershipUpdateEnquiryForm)
				&& Objects.equals(registrationForm, other.registrationForm)
				&& Objects.equals(signupEnquiryForm, other.signupEnquiryForm);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean enabled;
		private String logoUrl;
		private String registrationForm;
		private String signupEnquiryForm;
		private String membershipUpdateEnquiryForm;
		private List<String> attributes;
		private boolean enableSubprojects;

		private Builder()
		{
		}

		public Builder withEnabled(boolean enabled)
		{
			this.enabled = enabled;
			return this;
		}

		public Builder withLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
			return this;
		}

		public Builder withRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
			return this;
		}

		public Builder withSignupEnquiryForm(String signupEnquiryForm)
		{
			this.signupEnquiryForm = signupEnquiryForm;
			return this;
		}

		public Builder withMembershipUpdateEnquiryForm(String membershipUpdateEnquiryForm)
		{
			this.membershipUpdateEnquiryForm = membershipUpdateEnquiryForm;
			return this;
		}

		public Builder withAttributes(List<String> attributes)
		{
			this.attributes = Optional.ofNullable(attributes)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		public Builder withEnableSubprojects(boolean enableSubprojects)
		{
			this.enableSubprojects = enableSubprojects;
			return this;
		}

		public RestGroupDelegationConfiguration build()
		{
			return new RestGroupDelegationConfiguration(this);
		}
	}

}
