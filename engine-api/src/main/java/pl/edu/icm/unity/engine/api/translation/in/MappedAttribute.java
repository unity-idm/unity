/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import java.util.Objects;

import pl.edu.icm.unity.base.attribute.Attribute;

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
	public String toString()
	{
		return "MappedAttribute [mode=" + mode + ", attribute=" + attribute + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(attribute, mode);
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
		return Objects.equals(attribute, other.attribute) && mode == other.mode;
	}
}
