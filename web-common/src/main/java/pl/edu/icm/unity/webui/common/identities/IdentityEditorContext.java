/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

import com.vaadin.server.Sizeable.Unit;

/**
 * Contains complete information necessary to build identity editor UI.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class IdentityEditorContext
{
	private final boolean required;
	private final boolean adminMode;
	private final boolean showLabelInline;
	private Float customWidth = null;
	private Unit customWidthUnit = null;

	IdentityEditorContext(boolean required, boolean adminMode, boolean showLabelInline, Float customWidth,
			Unit customWidthUnit)
	{
		this.required = required;
		this.adminMode = adminMode;
		this.showLabelInline = showLabelInline;
		this.customWidth = customWidth;
		this.customWidthUnit = customWidthUnit;
	}

	public boolean isRequired()
	{
		return required;
	}

	public boolean isAdminMode()
	{
		return adminMode;
	}

	public boolean isShowLabelInline()
	{
		return showLabelInline;
	}
	
	public boolean isCustomWidth()
	{
		return customWidth != null && customWidthUnit != null;
	}

	public Float getCustomWidth()
	{
		return customWidth;
	}

	public Unit getCustomWidthUnit()
	{
		return customWidthUnit;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private boolean required = false;
		private boolean adminMode = false;
		private boolean showLabelInline = false;
		private Float customWidth = null;
		private Unit customWidthUnit = null;

		public Builder withRequired(boolean required)
		{
			this.required = required;
			return this;
		}

		public Builder withAdminMode(boolean adminMode)
		{
			this.adminMode = adminMode;
			return this;
		}

		public Builder withLabelInLine(boolean showLabelInline)
		{
			this.showLabelInline = showLabelInline;
			return this;
		}
		
		public Builder withCustomWidth(float width)
		{
			this.customWidth = width;
			return this;
		}
		
		public Builder withCustomWidthUnit(Unit unit)
		{
			this.customWidthUnit = unit;
			return this;
		}

		public IdentityEditorContext build()
		{
			return new IdentityEditorContext(required, adminMode, showLabelInline, 
					customWidth, customWidthUnit);
		}
	}
}
