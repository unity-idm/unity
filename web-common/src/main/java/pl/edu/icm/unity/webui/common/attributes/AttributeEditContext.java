/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.attributes;

import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Contain all necessary informations to build UI editor to attribute
 * 
 * @author P.Piernik
 *
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

	public static Builder builder()
	{
		return new Builder();
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

		public AttributeEditContext build()
		{
			return obj;
		}
	}

	public AttributeEditContext()
	{

	}

//	public AttributeEditContext(ConfirmationMode confirmationMode, boolean required,
//			AttributeType attributeType, EntityParam attributeOwner,
//			String attributeGroup)
//	{
//
//		this.confirmationMode = confirmationMode;
//		this.required = required;
//		this.attributeType = attributeType;
//		this.attributeOwner = attributeOwner;
//		this.attributeGroup = attributeGroup;
//	}

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
}
