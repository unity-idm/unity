/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DBGroupDelegationConfiguration.Builder.class)
class DBGroupDelegationConfiguration
{
	final boolean enabled;
	final String logoUrl;
	final String registrationForm;
	final String signupEnquiryForm;
	final String membershipUpdateEnquiryForm;
	final List<String> attributes;
	final List<Long> policyDocumentsIds;
	final boolean enableSubprojects;

	private DBGroupDelegationConfiguration(Builder builder)
	{
		this.enabled = builder.enabled;
		this.logoUrl = builder.logoUrl;
		this.registrationForm = builder.registrationForm;
		this.signupEnquiryForm = builder.signupEnquiryForm;
		this.membershipUpdateEnquiryForm = builder.membershipUpdateEnquiryForm;
		this.attributes = Optional.ofNullable(builder.attributes)
				.map(List::copyOf)
				.orElse(null);
		this.policyDocumentsIds = Optional.ofNullable(builder.policyDocumentsIds)
				.map(List::copyOf)
				.orElse(null);
		this.enableSubprojects = builder.enableSubprojects;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attributes, policyDocumentsIds, enableSubprojects, enabled, logoUrl, membershipUpdateEnquiryForm,
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
		DBGroupDelegationConfiguration other = (DBGroupDelegationConfiguration) obj;
		return Objects.equals(attributes, other.attributes) && Objects.equals(policyDocumentsIds, other.policyDocumentsIds)
				&& enableSubprojects == other.enableSubprojects
				&& enabled == other.enabled && Objects.equals(logoUrl, other.logoUrl)
				&& Objects.equals(membershipUpdateEnquiryForm, other.membershipUpdateEnquiryForm)
				&& Objects.equals(registrationForm, other.registrationForm)
				&& Objects.equals(signupEnquiryForm, other.signupEnquiryForm);
	}

	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private boolean enabled;
		private String logoUrl;
		private String registrationForm;
		private String signupEnquiryForm;
		private String membershipUpdateEnquiryForm;
		private List<String> attributes;
		private List<Long> policyDocumentsIds;
		private boolean enableSubprojects;

		private Builder()
		{
		}

		Builder withEnabled(boolean enabled)
		{
			this.enabled = enabled;
			return this;
		}

		Builder withLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
			return this;
		}

		Builder withRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
			return this;
		}

		Builder withSignupEnquiryForm(String signupEnquiryForm)
		{
			this.signupEnquiryForm = signupEnquiryForm;
			return this;
		}

		Builder withMembershipUpdateEnquiryForm(String membershipUpdateEnquiryForm)
		{
			this.membershipUpdateEnquiryForm = membershipUpdateEnquiryForm;
			return this;
		}

		Builder withAttributes(List<String> attributes)
		{
			this.attributes = Optional.ofNullable(attributes)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}
		
		Builder withPolicyDocumentsIds(List<Long> policyDocumentsIds)
		{
			this.policyDocumentsIds = Optional.ofNullable(policyDocumentsIds)
					.map(List::copyOf)
					.orElse(null);
			return this;
		}

		Builder withEnableSubprojects(boolean enableSubprojects)
		{
			this.enableSubprojects = enableSubprojects;
			return this;
		}

		DBGroupDelegationConfiguration build()
		{
			return new DBGroupDelegationConfiguration(this);
		}
	}

}