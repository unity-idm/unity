/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Identity registration option.
 * @author K. Benedyczak
 */
public class IdentityRegistrationParam extends RegistrationParam
{
	private String identityType;

	public String getIdentityType()
	{
		return identityType;
	}

	public void setIdentityType(String identityType)
	{
		this.identityType = identityType;
	}
}
