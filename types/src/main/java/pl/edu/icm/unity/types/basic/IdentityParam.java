/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;


/**
 * This class is useful when creating a new Identity.
 * 
 * @author K. Benedyczak
 */
public class IdentityParam extends IdentityTaV
{
	private boolean local;
	
	public IdentityParam()
	{
	}
	
	public IdentityParam(String type, String value, boolean local) 
	{
		super(type, value);
		this.local = local;
	}

	public boolean isLocal()
	{
		return local;
	}
	
	public void setLocal(boolean local)
	{
		this.local = local;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (local ? 1231 : 1237);
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
		IdentityParam other = (IdentityParam) obj;
		if (local != other.local)
			return false;
		return true;
	}
}
