/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import com.vaadin.server.Sizeable.Unit;

/**
 * Contains complete information necessary to build attribute viewer UI
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class AttributeViewerContext
{
	public static final AttributeViewerContext EMPTY = AttributeViewerContext.builder().build();
	
	private Float customWidth = null;
	private Unit customWidthUnit = null;
	private boolean showCaption = true;

	private AttributeViewerContext()
	{
	}

	public Float getCustomWidth()
	{
		return customWidth;
	}

	public Unit getCustomWidthUnit()
	{
		return customWidthUnit;
	}

	public boolean isCustomWidth()
	{
		return customWidth != null && customWidthUnit != null;
	}
	
	public boolean isShowCaption()
	{
		return showCaption;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static class Builder
	{
		private AttributeViewerContext obj;

		public Builder()
		{
			this.obj = new AttributeViewerContext();
		}

		public Builder withCustomWidth(float customWidth)
		{
			this.obj.customWidth = customWidth;
			return this;
		}

		public Builder withCustomWidthUnit(Unit customWidthUnit)
		{
			this.obj.customWidthUnit = customWidthUnit;
			return this;
		}
		
		public Builder withShowCaption(boolean showCaption)
		{
			this.obj.showCaption = showCaption;
			return this;
		}

		public AttributeViewerContext build()
		{
			return obj;
		}
	}

}
