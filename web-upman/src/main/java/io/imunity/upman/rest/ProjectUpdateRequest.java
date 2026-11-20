/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ProjectUpdateRequest.Builder.class)
record ProjectUpdateRequest(
		@JsonProperty("public") boolean isPublic,
		Map<String, String> displayedName,
		Map<String, String> description,
		boolean enableDelegation,
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
		private boolean isPublic;
		private Map<String, String> displayedName;
		private Map<String, String> description;
		private boolean enableDelegation;
		private String logoUrl;
		private boolean enableSubprojects;
		private List<String> readOnlyAttributes;

		private Builder()
		{
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

		Builder withEnableDelegation(boolean enableDelegation)
		{
			this.enableDelegation = enableDelegation;
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

		ProjectUpdateRequest build()
		{
			return new ProjectUpdateRequest(isPublic, displayedName, description, enableDelegation, logoUrl,
				enableSubprojects, readOnlyAttributes);
		}
	}
}
