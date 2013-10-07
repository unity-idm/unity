/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Attribute registration parameter. 
 * @author K. Benedyczak
 */
public class AttributeParamValue
{
	private Attribute<?> attribute;
	private boolean external;

	public Attribute<?> getAttribute()
	{
		return attribute;
	}
	public void setAttribute(Attribute<?> attribute)
	{
		this.attribute = attribute;
	}
	public boolean isExternal()
	{
		return external;
	}
	public void setExternal(boolean external)
	{
		this.external = external;
	}
}
