/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestAuthnGridSettings.Builder.class)
public class RestAuthnGridSettings
{
	public final boolean searchable;
	public final int height;

	private RestAuthnGridSettings(RestAuthnGridSettings.Builder builder)
	{
		this.searchable = builder.searchable;
		this.height = builder.height;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(height, searchable);
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
		RestAuthnGridSettings other = (RestAuthnGridSettings) obj;
		return height == other.height && searchable == other.searchable;
	}

	public static RestAuthnGridSettings.Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean searchable;
		private int height;

		private Builder()
		{
		}

		public RestAuthnGridSettings.Builder withSearchable(boolean searchable)
		{
			this.searchable = searchable;
			return this;
		}

		public RestAuthnGridSettings.Builder withHeight(int height)
		{
			this.height = height;
			return this;
		}

		public RestAuthnGridSettings build()
		{
			return new RestAuthnGridSettings(this);
		}
	}

}