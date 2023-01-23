/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;
import java.util.Objects;


@JsonDeserialize(builder = RestProject.RestProjectBuilder.class)
class RestProject
{
	final String projectId;
	@JsonProperty("public")
	final boolean isPublic;
	final Map<String, String> displayedName;
	final Map<String, String> description;
	final boolean enableDelegation;
	final String logoUrl;
	final boolean enableSubprojects;
	final List<String> readOnlyAttributes;
	final String registrationForm;
	final String signUpEnquiry;
	final String membershipUpdateEnquiry;

	RestProject(String projectId, boolean isPublic, Map<String, String> displayedName,
	            Map<String, String> description, boolean enableDelegation,
	            String logoUrl, boolean enableSubprojects, List<String> readOnlyAttributes, String registrationForm,
	            String signUpEnquiry, String membershipUpdateEnquiry)
	{
		this.projectId = projectId;
		this.isPublic = isPublic;
		this.displayedName = displayedName;
		this.description = description;
		this.enableDelegation = enableDelegation;
		this.logoUrl = logoUrl;
		this.enableSubprojects = enableSubprojects;
		this.readOnlyAttributes = readOnlyAttributes;
		this.registrationForm = registrationForm;
		this.signUpEnquiry = signUpEnquiry;
		this.membershipUpdateEnquiry = membershipUpdateEnquiry;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestProject that = (RestProject) o;
		return isPublic == that.isPublic && enableDelegation == that.enableDelegation &&
			enableSubprojects == that.enableSubprojects &&
			Objects.equals(projectId, that.projectId) &&
			Objects.equals(displayedName, that.displayedName) &&
			Objects.equals(description, that.description) &&
			Objects.equals(logoUrl, that.logoUrl) &&
			Objects.equals(readOnlyAttributes, that.readOnlyAttributes) &&
			Objects.equals(registrationForm, that.registrationForm) &&
			Objects.equals(signUpEnquiry, that.signUpEnquiry) &&
			Objects.equals(membershipUpdateEnquiry, that.membershipUpdateEnquiry);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(projectId, isPublic, displayedName, description, enableDelegation, logoUrl,
			enableSubprojects, readOnlyAttributes, registrationForm, signUpEnquiry, membershipUpdateEnquiry);
	}

	@Override
	public String toString()
	{
		return "RestProject{" +
			"groupName='" + projectId + '\'' +
			", isPublic=" + isPublic +
			", displayedName=" + displayedName +
			", description=" + description +
			", enableDelegation=" + enableDelegation +
			", logoUrl='" + logoUrl + '\'' +
			", enableSubprojects=" + enableSubprojects +
			", readOnlyAttributes='" + readOnlyAttributes + '\'' +
			", registrationForm='" + registrationForm + '\'' +
			", signUpEnquiry='" + signUpEnquiry + '\'' +
			", membershipUpdateEnquiry='" + membershipUpdateEnquiry + '\'' +
			'}';
	}

	public static RestProjectBuilder builder()
	{
		return new RestProjectBuilder();
	}

	public static final class RestProjectBuilder
	{
		private String projectId;
		private boolean isPublic;
		private Map<String, String> displayedName;
		private Map<String, String> description;
		private boolean enableDelegation;
		private String logoUrl;
		private boolean enableSubprojects;
		private List<String> readOnlyAttributes;
		private String registrationForm;
		private String signUpEnquiry;
		private String membershipUpdateEnquiry;
		private RestProjectBuilder()
		{
		}

		public RestProjectBuilder withProjectId(String groupName)
		{
			this.projectId = groupName;
			return this;
		}

		public RestProjectBuilder withPublic(boolean isPublic)
		{
			this.isPublic = isPublic;
			return this;
		}

		public RestProjectBuilder withDisplayedName(Map<String, String> displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		public RestProjectBuilder withDescription(Map<String, String> description)
		{
			this.description = description;
			return this;
		}

		public RestProjectBuilder withEnableDelegation(boolean enableDelegation)
		{
			this.enableDelegation = enableDelegation;
			return this;
		}

		public RestProjectBuilder withLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
			return this;
		}

		public RestProjectBuilder withEnableSubprojects(boolean enableSubprojects)
		{
			this.enableSubprojects = enableSubprojects;
			return this;
		}

		public RestProjectBuilder withReadOnlyAttributes(List<String> readOnlyAttributes)
		{
			this.readOnlyAttributes = readOnlyAttributes;
			return this;
		}

		public RestProjectBuilder withRegistrationForm(String registrationForm)
		{
			this.registrationForm = registrationForm;
			return this;
		}

		public RestProjectBuilder withSignUpEnquiry(String signUpEnquiry)
		{
			this.signUpEnquiry = signUpEnquiry;
			return this;
		}

		public RestProjectBuilder withMembershipUpdateEnquiry(String membershipUpdateEnquiry)
		{
			this.membershipUpdateEnquiry = membershipUpdateEnquiry;
			return this;
		}

		public RestProject build()
		{
			return new RestProject(projectId, isPublic, displayedName, description, enableDelegation, logoUrl,
				enableSubprojects, readOnlyAttributes, registrationForm, signUpEnquiry, membershipUpdateEnquiry);
		}
	}
}
