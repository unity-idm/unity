/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.attributes;

import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Contain all necessary informations to build UI editor to attribute
 * @author P.Piernik
 *
 */
public class AttributeEditContext
{
	public enum ConfirmationMode
	{
		ADMIN, USER, OFF, FORCE_CONFIRMED, 
	}
	
	private ConfirmationMode confirmationMode;
	private boolean required;
	private AttributeType attributeType; 
	private EntityParam attributeOwner;
	private String attributeGroup;
	
	public AttributeEditContext(ConfirmationMode confirmationMode, boolean required,
			AttributeType attributeType, EntityParam attributeOwner,
			String attributeGroup)
	{
		
		this.confirmationMode = confirmationMode;
		this.required = required;
		this.attributeType = attributeType;
		this.attributeOwner = attributeOwner;
		this.attributeGroup = attributeGroup;
	}
	
	
	public ConfirmationMode getConfirmationMode()
	{
		return confirmationMode;
	}
	public void setConfirmationMode(ConfirmationMode confirmationMode)
	{
		this.confirmationMode = confirmationMode;
	}
	public boolean isRequired()
	{
		return required;
	}
	public void setRequired(boolean required)
	{
		this.required = required;
	}

	public EntityParam getAttributeOwner()
	{
		return attributeOwner;
	}
	public void setAttributeOwner(EntityParam attributeOwner)
	{
		this.attributeOwner = attributeOwner;
	}
	public String getAttributeGroup()
	{
		return attributeGroup;
	}
	public void setAttributeGroup(String attributeGroup)
	{
		this.attributeGroup = attributeGroup;
	}
	public AttributeType getAttributeType()
	{
		return attributeType;
	}
	public void setAttributeType(AttributeType attributeType)
	{
		this.attributeType = attributeType;
	}
}
