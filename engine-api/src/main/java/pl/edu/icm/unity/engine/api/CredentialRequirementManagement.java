/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.Collection;

import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * API for {@link CredentialRequirements} management.
 * 
 * @author K. Benedyczak
 */
public interface CredentialRequirementManagement
{
	public static final String DEFAULT_CREDENTIAL_REQUIREMENT = "sys:all";
	
	/**
	 * Defines a new credential requirements instance
	 * @param name
	 * @param credentials
	 * @param description
	 * @throws EngineException
	 */
	void addCredentialRequirement(CredentialRequirements updated) throws EngineException;
	
	/**
	 * Updated a definitions of credential set. 
	 * @param updated updated data. The existing one is matched by id.
	 * @throws EngineException
	 */
	void updateCredentialRequirement(CredentialRequirements updated) throws EngineException;

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
	 * @param toRemove credential requirement to remove
	 * @param replacementId credential requirement to be applied to entities that used the removed requirement.
	 * Can be null, but then the operation will be successful only if there was no entity with the requirement. 
	 * @throws EngineException
	 */
	void removeCredentialRequirement(String toRemove, String replacementId) throws EngineException;
	
	/**
	 * @return collection of existing credential requirements
	 * @throws EngineException
	 */
	Collection<CredentialRequirements> getCredentialRequirements() throws EngineException;
	
	/**
	 * @return existing credential requirement
	 * @throws EngineException
	 */
	CredentialRequirements getCredentialRequirements(String name) throws EngineException;
}
