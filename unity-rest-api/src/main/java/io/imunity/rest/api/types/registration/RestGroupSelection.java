/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestGroupSelection.Builder.class)
public class RestGroupSelection
{
	public final List<String> selectedGroups;
	public final String externalIdp;
	public final String translationProfile;

	@Override
	public int hashCode()
	{
		return Objects.hash(externalIdp, selectedGroups, translationProfile);
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
		RestGroupSelection other = (RestGroupSelection) obj;
		return Objects.equals(externalIdp, other.externalIdp) && Objects.equals(selectedGroups, other.selectedGroups)
				&& Objects.equals(translationProfile, other.translationProfile);
	}

	private RestGroupSelection(Builder builder)
	{
		this.selectedGroups = Optional.ofNullable(builder.selectedGroups)
				.map(List::copyOf)
				.orElse(null);
		this.externalIdp = builder.externalIdp;
		this.translationProfile = builder.translationProfile;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private List<String> selectedGroups = Collections.emptyList();
		private String externalIdp;
		private String translationProfile;

		private Builder()
		{
		}

		public Builder withSelectedGroups(List<String> selectedGroups)
		{
			this.selectedGroups = selectedGroups;
			return this;
		}

		public Builder withExternalIdp(String externalIdp)
		{
			this.externalIdp = externalIdp;
			return this;
		}

		public Builder withTranslationProfile(String translationProfile)
		{
			this.translationProfile = translationProfile;
			return this;
		}

		public RestGroupSelection build()
		{
			return new RestGroupSelection(this);
		}
	}

}
