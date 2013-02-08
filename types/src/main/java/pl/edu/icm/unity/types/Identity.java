/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;

/**
 * Represents an identity with full information as returned from the engine.
 * 
 * @author K. Benedyczak
 */
public class Identity extends IdentityTaV
{
	private String entityId;
	private boolean enabled;
	private IdentityType type;
	
	public Identity(IdentityType type, String value, String entityId, boolean enabled) 
			throws IllegalIdentityValueException
	{
		super(type.getIdentityTypeProvider(), value);
		this.entityId = entityId;
		this.enabled = enabled;
		this.type = type;
	}

	public String getEntityId()
	{
		return entityId;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public IdentityType getType()
	{
		return type;
	}
	
	public List<Attribute<?>> extractAttributes()
	{
		return type.getIdentityTypeProvider().extractAttributes(getValue(), type.getExtractedAttributes());
	}
}
