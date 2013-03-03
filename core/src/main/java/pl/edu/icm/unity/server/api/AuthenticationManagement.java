/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Internal engine API for authentication management.
 * 
 * @author K. Benedyczak
 */
public interface AuthenticationManagement
{
	/**
	 * @param bindingId can be null to return all available authenticators. Otherwise filters the result
	 * to include only authenticators supporting the specified binding
	 * @return list of available authenticators 
	 * @throws EngineException
	 */
	public Collection<AuthenticatorTypeDescription> getAuthenticatorTypes(String bindingId) throws EngineException;
	
	/**
	 * @param bindingId if not null allows for filtering the returned list by supported binding
	 * @return list of currently configured authenticators
	 * @throws EngineException
	 */
	public Collection<AuthenticatorInstance> getAuthenticators(String bindingId) throws EngineException;
	
	/**
	 * Creates a new authenticator instance
	 * @param typeId authenticator type id
	 * @param jsonConfiguration configuration as JSON string
	 * @return the created authenticator
	 * @throws EngineException
	 */
	public AuthenticatorInstance createAuthenticator(String typeId, String jsonConfiguration) throws EngineException;
	
	/**
	 * Updates a configuration of an existing authenticator instance
	 * @param id
	 * @param jsonConfiguration
	 * @throws EngineException
	 */
	public void updateAuthenticator(String id, String jsonConfiguration) throws EngineException;
	
	/**
	 * Removes an existing authenticator. The authenticator must not be used by any of the endpoints,
	 * to be removed.
	 * @param id
	 * @throws EngineException
	 */
	public void removeAuthenticator(String id) throws EngineException;
	
	
	
	/**
	 * @return list of available credential types.
	 * @throws EngineException
	 */
	public Collection<CredentialType> getCredentialTypes() throws EngineException;

	/**
	 * Defines a new credential requirements instance
	 * @param name
	 * @param configuredCredentials
	 * @return
	 * @throws EngineException
	 */
	public CredentialRequirements addCredentialRequirement(String name,
			Collection<CredentialDefinition> configuredCredentials) throws EngineException;
	
	/**
	 * Updated a definitions of credential set. 
	 * @param updated updated data. The existing one is matched by id.
	 * @param desiredAuthnState The desired credential state to be applied to entities which 
	 * have this set set. If set to correct, then the operation will be successful only 
	 * if there is no entity with this set or if all entities have credentials fulfilling new rules.
	 * @throws EngineException
	 */
	public void updateCredentialRequirement(CredentialRequirements updated, 
			LocalCredentialState desiredAuthnState) throws EngineException;

	/**
	 * Removes the given credential set definition. The second argument is used to get another existing 
	 * set, to replace the removed one where it is used. It can be null only if the removed set is not used.
	 * @param toRemove
	 * @param replacementId
	 * @throws EngineException
	 */
	public void removeCredentialRequirement(String toRemove, String replacementId) throws EngineException;
	
	/**
	 * @return collection of existing credential requirements
	 * @throws EngineException
	 */
	public Collection<CredentialRequirements> getCredentialRequirements() throws EngineException;
}















