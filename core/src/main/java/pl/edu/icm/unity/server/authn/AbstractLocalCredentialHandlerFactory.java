/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.types.authn.CredentialType;

/**
 * Common code for most {@link LocalCredentialHandlerFactory}ies.
 * @author K. Benedyczak
 */
public abstract class AbstractLocalCredentialHandlerFactory implements LocalCredentialHandlerFactory
{
	protected final CredentialType type;
	
	protected AbstractLocalCredentialHandlerFactory(String name, String description)
	{
		type = new CredentialType(name, description);
	}
	
	@Override
	public String getName()
	{
		return type.getName();
	}

	@Override
	public String getDescription()
	{
		return type.getDescription();

	}

	@Override
	public CredentialType getCredentialType()
	{
		return type;
	}
}
