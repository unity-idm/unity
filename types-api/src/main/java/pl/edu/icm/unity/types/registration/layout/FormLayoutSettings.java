/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

import java.util.Objects;

import pl.edu.icm.unity.types.registration.BaseForm;

/**
 * Provides visual configuration that can be applied on a {@link BaseForm}.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class FormLayoutSettings
{
	public static final FormLayoutSettings DEFAULT = new FormLayoutSettings(false, 21, "em", false, null);

	private boolean compactInputs;
	private boolean showCancel;
	private float columnWidth;
	private String columnWidthUnit;
	private String logoURL;

	public FormLayoutSettings()
	{
	}

	public FormLayoutSettings(boolean compactInputs, float columnWidth, String columnWidthUnit, boolean showCancel,
			String logoURL)
	{
		this.compactInputs = compactInputs;
		this.columnWidth = columnWidth;
		this.columnWidthUnit = columnWidthUnit;
		this.showCancel = showCancel;
		this.logoURL = logoURL;
	}

	public boolean isCompactInputs()
	{
		return compactInputs;
	}

	public void setCompactInputs(boolean compactInputs)
	{
		this.compactInputs = compactInputs;
	}

	public float getColumnWidth()
	{
		return columnWidth;
	}

	public void setColumnWidth(float columnWidth)
	{
		this.columnWidth = columnWidth;
	}

	public String getColumnWidthUnit()
	{
		return columnWidthUnit;
	}

	public void setColumnWidthUnit(String columnWidthUnit)
	{
		this.columnWidthUnit = columnWidthUnit;
	}

	public boolean isShowCancel()
	{
		return showCancel;
	}
	
	public void setShowCancel(boolean showCancel)
	{
		this.showCancel = showCancel;
	}

	public String getLogoURL()
	{
		return logoURL;
	}
	
	public void setLogoURL(String logoURL)
	{
		this.logoURL = logoURL;
	}

	public static Builder builder()
	{
		return new Builder();
	}
	

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof FormLayoutSettings))
			return false;
		FormLayoutSettings castOther = (FormLayoutSettings) other;
		return Objects.equals(compactInputs, castOther.compactInputs)
				&& Objects.equals(showCancel, castOther.showCancel)
				&& Objects.equals(columnWidth, castOther.columnWidth)
				&& Objects.equals(columnWidthUnit, castOther.columnWidthUnit)
				&& Objects.equals(logoURL, castOther.logoURL);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(compactInputs, showCancel, columnWidth, columnWidthUnit, logoURL);
	}
	
	public static class Builder
	{
		private boolean compactInputs;
		private boolean showCancel;
		private float columnWidth;
		private String columnWidthUnit;
		private String logo;
		
		public Builder withCompactInputs(boolean compactInputs)
		{
			this.compactInputs = compactInputs;
			return this;
		}
		public Builder withColumnWidth(float columnWidth)
		{
			this.columnWidth = columnWidth;
			return this;
		}
		public Builder withColumnWidthUnit(String unit)
		{
			this.columnWidthUnit = unit;
			return this;
		}
		public Builder withShowCancel(boolean showCancel)
		{
			this.showCancel = showCancel;
			return this;
		}
		public Builder withLogo(String logo)
		{
			this.logo = logo;
			return this;
		}
		public FormLayoutSettings build()
		{
			return new FormLayoutSettings(compactInputs, columnWidth, columnWidthUnit, showCancel, logo);
		}
		
	}
}
