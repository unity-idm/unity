/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Allows for flexible addressing of a subject of a method operating on a particular identity:
 * either using identityId or using {@link IdentityValue}.
 * @author K. Benedyczak
 */
public class IdentityParam
{
	protected String identityId;
	protected IdentityTaV identityValue;
	
	public IdentityParam(String identityId)
	{
		this.identityId = identityId;
	}

	public IdentityParam(IdentityTaV identityValue)
	{
		this.identityValue = identityValue;
	}

	protected IdentityParam()
	{
	}
	
	public String getIdentityId()
	{
		return identityId;
	}

	public IdentityTaV getIdentityValue()
	{
		return identityValue;
	}
}
