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

@JsonDeserialize(builder = RestProjectCreateRequest.RestProjectBuilder.class)
class RestProjectCreateRequest
{
	final String projectId;
	@JsonProperty("public")
	final boolean isPublic;
	final Map<String, String> displayedName;
	final Map<String, String> description;
	final String logoUrl;
	final boolean enableSubprojects;
	final List<String> readOnlyAttributes;
	

	RestProjectCreateRequest(String projectId, boolean isPublic, Map<String, String> displayedName,
	                         Map<String, String> description, String logoUrl, boolean enableSubprojects,
	                         List<String> readOnlyAttributes)
	{
		this.projectId = projectId;
		this.isPublic = isPublic;
		this.displayedName = displayedName;
		this.description = description;
		this.logoUrl = logoUrl;
		this.enableSubprojects = enableSubprojects;
		this.readOnlyAttributes = readOnlyAttributes;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RestProjectCreateRequest that = (RestProjectCreateRequest) o;
		return isPublic == that.isPublic &&
			enableSubprojects == that.enableSubprojects &&
			Objects.equals(projectId, that.projectId) &&
			Objects.equals(displayedName, that.displayedName) &&
			Objects.equals(description, that.description) &&
			Objects.equals(logoUrl, that.logoUrl) &&
			Objects.equals(readOnlyAttributes, that.readOnlyAttributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(projectId, isPublic, displayedName, description, logoUrl,
			enableSubprojects, readOnlyAttributes);
	}

	@Override
	public String toString()
	{
		return "RestProject{" +
			"groupName='" + projectId + '\'' +
			", isPublic=" + isPublic +
			", displayedName=" + displayedName +
			", description=" + description +
			", logoUrl='" + logoUrl + '\'' +
			", enableSubprojects=" + enableSubprojects +
			", readOnlyAttributes='" + readOnlyAttributes + '\'' +
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
		private String logoUrl;
		private boolean enableSubprojects;
		private List<String> readOnlyAttributes;

		private RestProjectBuilder()
		{
		}

		public RestProjectBuilder withProjectId(String projectId)
		{
			this.projectId = projectId;
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

		public RestProjectCreateRequest build()
		{
			return new RestProjectCreateRequest(projectId, isPublic, displayedName, description, logoUrl,
				enableSubprojects, readOnlyAttributes);
		}
	}
}
