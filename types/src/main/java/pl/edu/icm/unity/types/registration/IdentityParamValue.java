/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Identity registration parameter. If the parameter was provided by an external IdP its name is set here too. 
 * @author K. Benedyczak
 */
public class IdentityParamValue extends IdentityParam
{
	private String externalIdp;

	public IdentityParamValue(String type, String value, String remoteIdp) 
	{
		super(type, value, false);
		this.externalIdp = remoteIdp;
	}

	public IdentityParamValue(String type, String value) 
	{
		super(type, value, true);
		this.externalIdp = null;
	}
	
	public IdentityParamValue() 
	{
	}	
	
	public String getExternalIdp()
	{
		return externalIdp;
	}
	public void setExternalIdp(String externalIdp)
	{
		this.externalIdp = externalIdp;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((externalIdp == null) ? 0 : externalIdp.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdentityParamValue other = (IdentityParamValue) obj;
		if (externalIdp == null)
		{
			if (other.externalIdp != null)
				return false;
		} else if (!externalIdp.equals(other.externalIdp))
			return false;
		return true;
	}
	
	/**
	 * @return full String representation
	 */
	public String toString()
	{
		return externalIdp == null ? super.toString() : 
			"[from: " + externalIdp + "] " + super.toString();
	}
}
