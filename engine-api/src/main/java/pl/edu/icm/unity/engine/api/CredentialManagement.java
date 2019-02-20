/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Credential management API
 * 
 * @author K. Benedyczak
 */
public interface CredentialManagement
{
	/**
	 * @return list of available credential types.
	 * @throws EngineException
	 */
	Collection<CredentialType> getCredentialTypes() throws EngineException;

	/**
	 * Defines a new credential definition, so it can be assigned to entities via credential requirements
	 * and to local authenticators.
	 * @param credentialDefinition
	 * @throws EngineException
	 */
	void addCredentialDefinition(CredentialDefinition credentialDefinition) 
			throws EngineException;

	/**
	 * Updates a definition of a credential. 
	 * @param updated updated data. The existing one is matched by name.
	 * @param desiredCredState The desired credential state to be applied to entities which 
	 * have this credential currently set. If value is 'correct', 
	 * then the operation will be successful only if all entities which have this credential 
	 * are fulfilling the new rules. If the value is 'outdated' then all identities which have 
	 * this credential set will have the state changed to 'valid' if their credentials fulfill 
	 * the rules of the new requirements or to 'outdated' otherwise. 
	 * The 'notSet' value means that the current credentials should have their values cleared.
	 * @throws EngineException
	 */
	void updateCredentialDefinition(CredentialDefinition updated, 
			LocalCredentialState desiredCredState) throws EngineException;

	/**
	 * Removes the given credential definition. The operation will be successful only if the credential 
	 * is not used by neither existing authenticators nor existing credential requirements.  
	 * 
	 * @param toRemove
	 * @throws EngineException
	 */
	void removeCredentialDefinition(String toRemove) throws EngineException;

	/**
	 * @return collection of existing credential definitions
	 * @throws EngineException
	 */
	Collection<CredentialDefinition> getCredentialDefinitions() throws EngineException;
	
	/**
	 * @return Credential definition with given name
	 * @throws EngineException
	 */
	CredentialDefinition getCredentialDefinition(String name) throws EngineException;
}
