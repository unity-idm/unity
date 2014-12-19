/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.verifiers;


/**
 * IdentityEmail verification facility.
 * @author P. Piernik
 */
public class IdentityEmailVerificator implements EmailVerificator
{

	public static final String NAME = "identityVerificator";
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verifi EmailIdentity";
	}

	@Override
	public boolean verify(ConfirmationState state)
	{
		// TODO Auto-generated method stub
		return false;
	}

}
