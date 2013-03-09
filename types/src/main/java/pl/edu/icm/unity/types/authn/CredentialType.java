/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import pl.edu.icm.unity.types.DescribedObjectROImpl;


/**
 * Describes credential type as username and password or one-time password.
 * @author K. Benedyczak
 */
public class CredentialType extends DescribedObjectROImpl
{
	public CredentialType()
	{
		super();
	}

	public CredentialType(String name, String description)
	{
		super(name, description);
	}
	
}
