/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.Unit;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;

public class AttributeEditContext
{
	private ConfirmationEditMode confirmationMode = ConfirmationEditMode.USER;
	private boolean required = false;
	private AttributeType attributeType;
	private EntityParam attributeOwner;
	private String attributeGroup;
	private LabelContext labelContext;
	private boolean showLabelInline = false;
	private Float customWidth = null;
	private Unit customWidthUnit = null;

	private AttributeEditContext()
	{
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public ConfirmationEditMode getConfirmationMode()
	{
		return confirmationMode;
	}

	public boolean isRequired()
	{
		return required;
	}

	public EntityParam getAttributeOwner()
	{
		return attributeOwner;

	}

	public String getAttributeGroup()
	{
		return attributeGroup;
	}

	public LabelContext getLabelContext()
	{
		return labelContext;
	}

	public AttributeType getAttributeType()
	{
		return attributeType;
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

	public static class Builder
	{
		private AttributeEditContext obj;

		public Builder()
		{
			this.obj = new AttributeEditContext();
		}

		public Builder withConfirmationMode(ConfirmationEditMode mode)
		{
			this.obj.confirmationMode = mode;
			return this;
		}

		public Builder required()
		{
			this.obj.required = true;
			return this;
		}
		
		public Builder withRequired(boolean required)
		{
			this.obj.required = required;
			return this;
		}

		public Builder withAttributeType(AttributeType type)
		{
			this.obj.attributeType = type;
			return this;
		}

		public Builder withAttributeOwner(EntityParam owner)
		{
			this.obj.attributeOwner = owner;
			return this;
		}

		public Builder withAttributeGroup(String group)
		{
			this.obj.attributeGroup = group;
			return this;
		}

		public Builder withLabelContext(LabelContext context)
		{
			this.obj.labelContext = context;
			return this;
		}
		
		public Builder withLabelInline(boolean showLabelInline)
		{
			this.obj.showLabelInline = showLabelInline;
			return this;
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

		public AttributeEditContext build()
		{
			return obj;
		}
	}
}
