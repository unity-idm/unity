/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Mapped {@link Attribute} with {@link AttributeEffectMode}.
 * @author K. Benedyczak
 */
public class MappedAttribute
{
	private AttributeEffectMode mode;
	private Attribute attribute;
	
	public MappedAttribute(AttributeEffectMode mode, Attribute attribute)
	{
		super();
		this.mode = mode;
		this.attribute = attribute;
	}

	public AttributeEffectMode getMode()
	{
		return mode;
	}

	public Attribute getAttribute()
	{
		return attribute;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MappedAttribute other = (MappedAttribute) obj;
		if (attribute == null)
		{
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (mode != other.mode)
			return false;
		return true;
	}
}
