/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.attributes.edit;

import com.vaadin.server.Sizeable.Unit;

import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Contains complete information necessary to build attribute editor UI
 * 
 * @author P.Piernik
 */
public class AttributeEditContext
{
	public enum ConfirmationMode
	{
		ADMIN, USER, OFF, FORCE_CONFIRMED,
	}
	
	private ConfirmationMode confirmationMode = ConfirmationMode.USER;
	private boolean required = false;
	private AttributeType attributeType;
	private EntityParam attributeOwner;
	private String attributeGroup;
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

	public ConfirmationMode getConfirmationMode()
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

		public Builder withConfirmationMode(ConfirmationMode mode)
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
