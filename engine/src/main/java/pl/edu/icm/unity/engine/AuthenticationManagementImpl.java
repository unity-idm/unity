/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Authentication management implementation.
 * @author K. Benedyczak
 */
@Component
public class AuthenticationManagementImpl implements AuthenticationManagement
{
	@Override
	public Collection<AuthenticatorTypeDescription> getAuthenticatorTypes(String bindingId)
			throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<AuthenticatorInstance> getAuthenticators(String bindingId)
			throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AuthenticatorInstance createAuthenticator(String typeId, String jsonConfiguration)
			throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateAuthenticator(String id, String jsonConfiguration) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAuthenticator(String id) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<CredentialType> getCredentialTypes() throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CredentialRequirements addCredentialRequirement(String name,
			Collection<CredentialDefinition> configuredCredentials)
			throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateCredentialRequirement(CredentialRequirements updated,
			LocalCredentialState desiredAuthnState) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeCredentialRequirement(String toRemove, String replacementId)
			throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<CredentialRequirements> getCredentialSetDefinitions()
			throws EngineException
	{
		// TODO Auto-generated method stub
		return null;
	}

}
