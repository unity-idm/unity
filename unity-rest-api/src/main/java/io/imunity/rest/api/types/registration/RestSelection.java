/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestSelection.Builder.class)
public class RestSelection
{
	public final boolean selected;
	public final String externalIdp;
	public final String translationProfile;

	private RestSelection(Builder builder)
	{
		this.selected = builder.selected;
		this.externalIdp = builder.externalIdp;
		this.translationProfile = builder.translationProfile;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(externalIdp, selected, translationProfile);
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
		RestSelection other = (RestSelection) obj;
		return Objects.equals(externalIdp, other.externalIdp) && selected == other.selected
				&& Objects.equals(translationProfile, other.translationProfile);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean selected;
		private String externalIdp;
		private String translationProfile;

		private Builder()
		{
		}

		public Builder withSelected(boolean selected)
		{
			this.selected = selected;
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

		public RestSelection build()
		{
			return new RestSelection(this);
		}
	}

}
