/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.types.authn.CredentialType;

/**
 * Base for {@link LocalCredentialHandler}s.
 * @author K. Benedyczak
 */
public abstract class AbstractLocalCredentialHandler implements LocalCredentialHandler
{
	protected CredentialType type;
	
	protected AbstractLocalCredentialHandler(CredentialType credType)
	{
		this.type = credType;
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
