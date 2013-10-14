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
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + (external ? 1231 : 1237);
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
		AttributeParamValue other = (AttributeParamValue) obj;
		if (attribute == null)
		{
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (external != other.external)
			return false;
		return true;
	}
}
