/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains configuration of group delegation
 * 
 * @author P.Piernik
 *
 */
public class GroupDelegationConfiguration
{
	public final boolean enabled;
	public final String logoUrl;
	public final String registrationForm;
	public final String signupEnquiryForm;
	public final String membershipUpdateEnquiryForm;
	public final List<String> attributes;
	public final List<Long> policyDocumentsIds;
	public final boolean enableSubprojects;
	
	public GroupDelegationConfiguration(boolean enabled)
	{
		this(enabled, false, null, null, null, null, null, null);
	}

	@JsonCreator
	public GroupDelegationConfiguration(@JsonProperty("enabled") boolean enabled,
			@JsonProperty( value = "enableSubprojects", required=false) Boolean enableSubprojects,
			@JsonProperty("logoUrl") String logoUrl,
			@JsonProperty("registrationForm") String registrationForm,
			@JsonProperty("signupEnquiryForm") String signupEnquiryForm,
			@JsonProperty("membershipUpdateEnquiryForm") String membershipUpdateEnquiryForm,
			@JsonProperty("attributes") List<String> attributes,
			@JsonProperty("policyDocumentsIds") List<Long> policyDocumentsIds)
	{
		this.enabled = enabled;
		this.logoUrl = logoUrl;
		this.registrationForm = registrationForm;
		this.signupEnquiryForm = signupEnquiryForm;
		this.membershipUpdateEnquiryForm = membershipUpdateEnquiryForm;
		this.attributes = attributes != null ? new ArrayList<>(attributes) : null;
		this.policyDocumentsIds = policyDocumentsIds != null ? new ArrayList<>(policyDocumentsIds) : null;
		this.enableSubprojects = enableSubprojects != null ? enableSubprojects : false;
	}
	
	private GroupDelegationConfiguration(Builder builder)
	{
		this.enabled = builder.enabled;
		this.logoUrl = builder.logoUrl;
		this.registrationForm = builder.registrationForm;
		this.signupEnquiryForm = builder.signupEnquiryForm;
		this.membershipUpdateEnquiryForm = builder.membershipUpdateEnquiryForm;
		this.attributes = builder.attributes != null ? List.copyOf(builder.attributes) : null;
		this.policyDocumentsIds = builder.policyDocumentsIds != null ? List.copyOf(builder.policyDocumentsIds) : null;
		this.enableSubprojects = builder.enableSubprojects;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.enabled, this.logoUrl, this.registrationForm, this.signupEnquiryForm,
				this.membershipUpdateEnquiryForm, this.attributes, this.policyDocumentsIds, this.enableSubprojects);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final GroupDelegationConfiguration other = (GroupDelegationConfiguration) obj;
		return Objects.equals(this.enabled, other.enabled)
				&& Objects.equals(this.registrationForm, other.registrationForm)
				&& Objects.equals(this.signupEnquiryForm, other.signupEnquiryForm)
				&& Objects.equals(this.logoUrl, other.logoUrl)
				&& Objects.equals(this.membershipUpdateEnquiryForm, other.membershipUpdateEnquiryForm)
				&& Objects.equals(this.attributes, other.attributes)
				&& Objects.equals(this.policyDocumentsIds, other.policyDocumentsIds)
				&& Objects.equals(this.enableSubprojects, other.enableSubprojects);

	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean enabled = true;
		private String logoUrl;
		private String registrationForm;
		private String signupEnquiryForm;
		private String membershipUpdateEnquiryForm;
		private List<String> attributes = null;
		private List<Long> policyDocumentsIds = null;
		private boolean enableSubprojects = false;

		private Builder()
		{
		}

		public Builder withEnabled(boolean enabled)
		{
			this.enabled = enabled;
			return this;
		}

		public Builder copy(GroupDelegationConfiguration toCopy)
		{
			this.enabled = toCopy.enabled;
			this.logoUrl = toCopy.logoUrl;
			this.registrationForm = toCopy.registrationForm;
			this.signupEnquiryForm = toCopy.signupEnquiryForm;
			this.membershipUpdateEnquiryForm = toCopy.membershipUpdateEnquiryForm;
			this.attributes = toCopy.attributes == null ? null : new ArrayList<>(toCopy.attributes);
			this.policyDocumentsIds = toCopy.policyDocumentsIds == null ? null : new ArrayList<>(toCopy.policyDocumentsIds);
			this.enableSubprojects = toCopy.enableSubprojects;
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
			this.attributes = attributes;
			return this;
		}

		public Builder withPolicyDocumentsIds(List<Long> policyDocumentsIds)
		{
			this.policyDocumentsIds = policyDocumentsIds;
			return this;
		}

		public Builder withEnableSubprojects(boolean enableSubprojects)
		{
			this.enableSubprojects = enableSubprojects;
			return this;
		}

		public GroupDelegationConfiguration build()
		{
			return new GroupDelegationConfiguration(this);
		}
	}
}
