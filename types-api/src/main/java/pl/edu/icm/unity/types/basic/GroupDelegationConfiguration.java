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

	public GroupDelegationConfiguration(boolean enabled)
	{
		this(enabled, null, null, null, null, null);
	}

	@JsonCreator
	public GroupDelegationConfiguration(@JsonProperty("enabled") boolean enabled,
			@JsonProperty("logoUrl") String logoUrl,
			@JsonProperty("registrationForm") String registrationForm,
			@JsonProperty("signupEnquiryForm") String signupEnquiryForm,
			@JsonProperty("membershipUpdateEnquiryForm") String membershipUpdateEnquiryForm,
			@JsonProperty("attributes") List<String> attributes)
	{
		this.enabled = enabled;
		this.logoUrl = logoUrl;
		this.registrationForm = registrationForm;
		this.signupEnquiryForm = signupEnquiryForm;
		this.membershipUpdateEnquiryForm = membershipUpdateEnquiryForm;
		this.attributes = attributes != null ? new ArrayList<>(attributes) : null;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.enabled, this.logoUrl, this.registrationForm, this.signupEnquiryForm,
				this.membershipUpdateEnquiryForm, this.attributes);
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
				&& Objects.equals(this.attributes, other.attributes);

	}
}
