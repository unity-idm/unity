/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;

/**
 * Supports {@link PasswordExchange} and verifies the password and username against a configured LDAP 
 * server. Access to remote attribute and groups is also provided.
 * 
 * @author K. Benedyczak
 */
public class LdapVerificator extends AbstractRemoteVerificator implements PasswordExchange
{

	public LdapVerificator(String name, String description)
	{
		super(name, description, PasswordExchange.ID);
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public AuthenticatedEntity checkPassword(String username, String password)
			throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CredentialReset getCredentialResetBackend()
	{
		return new NoCredentialResetImpl();
	}
}
