/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ProjectCreateRequest.Builder.class)
record ProjectCreateRequest(
		String projectId,
		@JsonProperty("public") boolean isPublic,
		Map<String, String> displayedName,
		Map<String, String> description,
		String logoUrl,
		boolean enableSubprojects,
		List<String> readOnlyAttributes)
{
	static Builder builder()
	{
		return new Builder();
	}

	static final class Builder
	{
		private String projectId;
		private boolean isPublic;
		private Map<String, String> displayedName;
		private Map<String, String> description;
		private String logoUrl;
		private boolean enableSubprojects;
		private List<String> readOnlyAttributes;

		private Builder()
		{
		}

		Builder withProjectId(String projectId)
		{
			this.projectId = projectId;
			return this;
		}

		Builder withPublic(boolean isPublic)
		{
			this.isPublic = isPublic;
			return this;
		}

		Builder withDisplayedName(Map<String, String> displayedName)
		{
			this.displayedName = displayedName;
			return this;
		}

		Builder withDescription(Map<String, String> description)
		{
			this.description = description;
			return this;
		}

		Builder withLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
			return this;
		}

		Builder withEnableSubprojects(boolean enableSubprojects)
		{
			this.enableSubprojects = enableSubprojects;
			return this;
		}

		Builder withReadOnlyAttributes(List<String> readOnlyAttributes)
		{
			this.readOnlyAttributes = readOnlyAttributes;
			return this;
		}

		ProjectCreateRequest build()
		{
			return new ProjectCreateRequest(projectId, isPublic, displayedName, description, logoUrl,
				enableSubprojects, readOnlyAttributes);
		}
	}
}
