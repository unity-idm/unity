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
	private boolean enabled;
	private boolean local;
	
	public IdentityParam()
	{
	}
	
	public IdentityParam(String type, String value, boolean enabled, boolean local) 
	{
		super(type, value);
		this.enabled = enabled;
		this.local = local;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public boolean isLocal()
	{
		return local;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public void setLocal(boolean local)
	{
		this.local = local;
	}
}
