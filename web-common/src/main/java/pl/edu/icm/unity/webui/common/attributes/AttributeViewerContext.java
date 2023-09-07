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
	private Float customHeight = null;
	private Unit customHeightUnit = null;
	private Integer imageScaleWidth = null;
	private Integer imageScaleHeight = null;
	private boolean showCaption = true;
	private Integer maxTextSize = null;
	private boolean showAsLabel = false;
	private boolean showConfirmation = true;

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

	public Float getCustomHeight()
	{
		return customHeight;
	}

	public Unit getCustomHeightUnit()
	{
		return customHeightUnit;
	}

	public boolean isCustomHeight()
	{
		return customHeight != null && customHeightUnit != null;
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

	public boolean isShowConfirmation()
	{
		return showConfirmation;
	}

	public void setShowConfirmation(boolean showConfirmation)
	{
		this.showConfirmation = showConfirmation;
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
		
		public Builder withShowConfirmation(boolean showConfirmation)
		{
			this.obj.showConfirmation = showConfirmation;
			return this;
		}

		public AttributeViewerContext build()
		{
			return obj;
		}
	}

}
