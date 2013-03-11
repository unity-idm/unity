/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Collection;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;

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
	 * @param jsonVerificatorConfig configuration of verificator as JSON string
	 * @param jsonRetrievalConfig configuration of retrieval as JSON string
	 * @param credentialId name of the local credential, in case when the verificator used
	 * is validating local credentials. Otherwise ignored, can be null.
	 * @return the created authenticator
	 * @throws EngineException
	 */
	public AuthenticatorInstance createAuthenticator(String id, String typeId, String jsonVerificatorConfig,
			String jsonRetrievalConfig, String credentialId) throws EngineException;
	
	/**
	 * Updates a configuration of an existing authenticator instance
	 * @param id
	 * @param jsonVerificatorConfig configuration of verificator as JSON string
	 * @param jsonRetrievalConfig configuration of retrieval as JSON string
	 * @throws EngineException
	 */
	public void updateAuthenticator(String id, String jsonVerificatorConfig,
			String jsonRetrievalConfig) throws EngineException;
	
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
	 * @param description
	 * @return
	 * @throws EngineException
	 */
	public CredentialRequirements addCredentialRequirement(String name, String description,
			Set<CredentialDefinition> configuredCredentials) throws EngineException;
	
	/**
	 * Updated a definitions of credential set. 
	 * @param updated updated data. The existing one is matched by id.
	 * @param desiredAuthnState The desired credential state to be applied to entities which 
	 * have this requirement currently set. If value is 'valid', then the operation will be successful only 
	 * if there is no entity with this set or if all entities have credentials fulfilling new rules.
	 * If the value is 'outdated' then all identities which have this requirement set will have the state changed to 
	 * 'valid' if their credentials fulfill the rules of the new requirements or to 'outdated' otherwise.
	 * The 'disabled' value is always set.
	 * @throws EngineException
	 */
	public void updateCredentialRequirement(CredentialRequirements updated, 
			LocalAuthenticationState desiredAuthnState) throws EngineException;

	/**
	 * Removes the given credential set definition. The second argument is used to get another existing 
	 * set, to replace the removed one where it is used. It can be null only if the removed set is not used
	 * otherwise an exception is thrown.
	 * If the replacementId is not null, then the replacementAuthnState governs the desired overall authentication
	 * state of the entities which have their credential requirements modified. For details see 
	 * {@link #updateCredentialRequirement(CredentialRequirements, LocalAuthenticationState)} 
	 * - the semantics is the same here.
	 * 
	 * 
	 * @param toRemove
	 * @param replacementId
	 * @throws EngineException
	 */
	public void removeCredentialRequirement(String toRemove, String replacementId, 
			LocalAuthenticationState replacementAuthnState) throws EngineException;
	
	/**
	 * @return collection of existing credential requirements
	 * @throws EngineException
	 */
	public Collection<CredentialRequirements> getCredentialRequirements() throws EngineException;
}















