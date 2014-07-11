/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Mapped {@link Attribute} with {@link AttributeEffectMode}.
 * @author K. Benedyczak
 */
public class MappedAttribute
{
	private AttributeEffectMode mode;
	private Attribute<?> attribute;
	
	public MappedAttribute(AttributeEffectMode mode, Attribute<?> attribute)
	{
		super();
		this.mode = mode;
		this.attribute = attribute;
	}

	public AttributeEffectMode getMode()
	{
		return mode;
	}

	public Attribute<?> getAttribute()
	{
		return attribute;
	}
}
