/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * API for management of entities' credentials.
 * @author K. Benedyczak
 */
public interface EntityCredentialManagement
{
	/**
	 * Changes {@link CredentialRequirements} of an entity. 
	 * @param entity to be modified
	 * @param requirementId to be set
	 * @throws EngineException
	 */
	void setEntityCredentialRequirements(EntityParam entity, String requirementId) throws EngineException;
	
	/**
	 * Sets authentication secretes for the entity. After the change, the credential will be in correct state.  
	 * @param entity to be modified
	 * @param credentialId credential id to be changed. 
	 * @param secrets the credential type specific value of the credential.
	 * @throws EngineException
	 */
	void setEntityCredential(EntityParam entity, String credentialId, String secrets) throws EngineException;

	/**
	 * Sets local credential state. 
	 * @param entity to be modified
	 * @param credentialId credential id to be changed. 
	 * @param desiredCredentialState desired credential state. If 'notSet' then the current credential 
	 * is removed. The status can be set to 'outdated' only if the credential
	 * supports invalidation and currently there is a (correct or outdated) credential set. The 'correct'
	 * value is not allowed, and will cause an exception. Credential can be put into correct state with 
	 * {@link #setEntityCredential(EntityParam, String, String)}.  
	 * @throws EngineException
	 */
	void setEntityCredentialStatus(EntityParam entity, String credentialId,  
			LocalCredentialState desiredCredentialState) throws EngineException;
}

