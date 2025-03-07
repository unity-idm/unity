/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.Unit;


public class AttributeViewerContext
{
	public static final AttributeViewerContext EMPTY = AttributeViewerContext.builder().build();

	private Float customWidth = null;
	private Unit customWidthUnit = null;
	private Float customHeight = null;
	private Unit customHeightUnit = null;
	private Integer imageScaleWidth = null;
	private Integer imageScaleHeight = null;
	private boolean showCaption = true;
	private Integer maxTextSize = null;
	private boolean showAsLabel = false;
	private Float borderRadius = null;
	private Unit borderUnit = null;
	private String customWidthAsString = null;
	private Float customMaxWidth = null;
	private Unit customMaxWidthUnit = null;
	private Float customMaxHeight = null;
	private Unit customMaxHeightUnit = null;
	private boolean showDescriptionAsTooltip = false;

	
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

	public Float getCustomMaxWidth()
	{
		return customMaxWidth;
	}

	public Unit getCustomMaxWidthUnit()
	{
		return customMaxWidthUnit;
	}
	
	public boolean isCustomWidth()
	{
		return (customWidth != null && customWidthUnit != null) || customWidthAsString != null;
	}
	
	public boolean isCustomMaxWidth()
	{
		return customMaxWidth != null && customMaxWidthUnit != null;
	}
	
	public boolean isCustomWidthAsString()
	{
		return customWidthAsString != null;
	}

	public boolean isShowCaption()
	{
		return showCaption;
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public Float getCustomHeight()
	{
		return customHeight;
	}

	public Unit getCustomHeightUnit()
	{
		return customHeightUnit;
	}

	public Float getCustomMaxHeight()
	{
		return customMaxHeight;
	}

	public Unit getCustomMaxHeightUnit()
	{
		return customMaxHeightUnit;
	}
	
	public boolean isCustomHeight()
	{
		return customHeight != null && customHeightUnit != null;
	}

	public boolean isCustomMaxHeight()
	{
		return customMaxHeight != null && customMaxHeightUnit != null;
	}
	
	public Integer getMaxTextSize()
	{
		return maxTextSize;
	}

	public boolean isShowAsLabel()
	{
		return showAsLabel;
	}

	public Integer getImageScaleWidth()
	{
		return imageScaleWidth;
	}

	public Integer getImageScaleHeight()
	{
		return imageScaleHeight;
	}

	public boolean isScaleImage()
	{
		return imageScaleWidth != null && imageScaleHeight != null;
	}

	public Float getBorderRadius()
	{
		return borderRadius;
	}

	public Unit getBorderUnit()
	{
		return borderUnit;
	}
	
	public String getCustomWidthAsString()
	{
		return customWidthAsString;
	}

	public boolean isShowDescriptionAsTooltip()
	{
		return showDescriptionAsTooltip;
	}

	public static class Builder
	{
		private final AttributeViewerContext obj;

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
		
		public Builder withCustomWidth(String customWidth)
		{
			this.obj.customWidthAsString = customWidth;
			return this;
		}

		public Builder withCustomHeight(float customHeight)
		{
			this.obj.customHeight = customHeight;
			return this;
		}

		public Builder withCustomHeightUnit(Unit customHeightUnit)
		{
			this.obj.customHeightUnit = customHeightUnit;
			return this;
		}
		
		public Builder withCustomMaxWidth(float customWidth)
		{
			this.obj.customMaxWidth = customWidth;
			return this;
		}

		public Builder withCustomMaxWidthUnit(Unit customWidthUnit)
		{
			this.obj.customMaxWidthUnit = customWidthUnit;
			return this;
		}

		public Builder withCustomMaxHeight(float customHeight)
		{
			this.obj.customMaxHeight = customHeight;
			return this;
		}

		public Builder withCustomMaxHeightUnit(Unit customHeightUnit)
		{
			this.obj.customMaxHeightUnit = customHeightUnit;
			return this;
		}

		public Builder withMaxTextSize(int maxTextSize)
		{
			this.obj.maxTextSize = maxTextSize;
			return this;
		}

		public Builder withShowAsLabel(boolean asLabel)
		{
			this.obj.showAsLabel = asLabel;
			return this;
		}

		public Builder withShowCaption(boolean showCaption)
		{
			this.obj.showCaption = showCaption;
			return this;
		}

		public Builder withImageScaleWidth(int imageScaleWidth)
		{
			this.obj.imageScaleWidth = imageScaleWidth;
			return this;
		}

		public Builder withImageScaleHeight(int imageScaleHeight)
		{
			this.obj.imageScaleHeight = imageScaleHeight;
			return this;
		}

		public Builder withBorderRadius(float borderRadius)
		{
			this.obj.borderRadius = borderRadius;
			return this;
		}

		public Builder withBorderRadiusUnit(Unit borderUnit)
		{
			this.obj.borderUnit = borderUnit;
			return this;
		}
		
		public Builder withShowDescriptionAsTooltip(boolean show)
		{
			this.obj.showDescriptionAsTooltip = show;
			return this;
		}


		public AttributeViewerContext build()
		{
			return obj;
		}
	}

}
