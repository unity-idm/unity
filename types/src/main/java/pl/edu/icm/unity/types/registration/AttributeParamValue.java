/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Attribute registration parameter. If the parameter was provided by an external IdP its name is set here too.
 * TODO - this class should be removed. The externalIdp is already stored in Attribute, so it makes no sense. 
 * @author K. Benedyczak
 */
public class AttributeParamValue
{
	private Attribute<?> attribute;
	private String externalIdp;

	public Attribute<?> getAttribute()
	{
		return attribute;
	}
	public void setAttribute(Attribute<?> attribute)
	{
		this.attribute = attribute;
	}
	public String getExternalIdp()
	{
		return attribute.getRemoteIdp();
	}
	public void setExternalIdp(String externalIdp)
	{
		attribute.setRemoteIdp(externalIdp);
		this.externalIdp = externalIdp;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + ((externalIdp == null) ? 0 : externalIdp.hashCode());
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
		if (externalIdp == null)
		{
			if (other.externalIdp != null)
				return false;
		} else if (!externalIdp.equals(other.externalIdp))
			return false;
		return true;
	}
}
