/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.layout;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestFormLayoutSettings.Builder.class)
public class RestFormLayoutSettings
{
	public final boolean compactInputs;
	public final boolean showCancel;
	public final float columnWidth;
	public final String columnWidthUnit;
	public final String logoURL;

	private RestFormLayoutSettings(Builder builder)
	{
		this.compactInputs = builder.compactInputs;
		this.showCancel = builder.showCancel;
		this.columnWidth = builder.columnWidth;
		this.columnWidthUnit = builder.columnWidthUnit;
		this.logoURL = builder.logoURL;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(columnWidth, columnWidthUnit, compactInputs, logoURL, showCancel);
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
		RestFormLayoutSettings other = (RestFormLayoutSettings) obj;
		return Float.floatToIntBits(columnWidth) == Float.floatToIntBits(other.columnWidth)
				&& Objects.equals(columnWidthUnit, other.columnWidthUnit) && compactInputs == other.compactInputs
				&& Objects.equals(logoURL, other.logoURL) && showCancel == other.showCancel;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private boolean compactInputs;
		private boolean showCancel;
		private float columnWidth;
		private String columnWidthUnit;
		private String logoURL;

		private Builder()
		{
		}

		public Builder withCompactInputs(boolean compactInputs)
		{
			this.compactInputs = compactInputs;
			return this;
		}

		public Builder withShowCancel(boolean showCancel)
		{
			this.showCancel = showCancel;
			return this;
		}

		public Builder withColumnWidth(float columnWidth)
		{
			this.columnWidth = columnWidth;
			return this;
		}

		public Builder withColumnWidthUnit(String columnWidthUnit)
		{
			this.columnWidthUnit = columnWidthUnit;
			return this;
		}

		public Builder withLogoURL(String logoURL)
		{
			this.logoURL = logoURL;
			return this;
		}

		public RestFormLayoutSettings build()
		{
			return new RestFormLayoutSettings(this);
		}
	}

}
