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
}
