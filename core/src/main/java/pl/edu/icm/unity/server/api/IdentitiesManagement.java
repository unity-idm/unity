/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Internal engine API for entities and identities management.
 * @author K. Benedyczak
 */
public interface IdentitiesManagement
{
	/**
	 * @return list of supported identity types
	 * @throws EngineException
	 */
	public List<IdentityType> getIdentityTypes() throws EngineException;
	
	/**
	 * Allows to update mutable part of identity type, as extracted fields or description.
	 * @param toUpdate
	 * @throws EngineException
	 */
	public void updateIdentityType(IdentityType toUpdate) throws EngineException;
	
	/**
	 * Adds a new entity with an initial identity.
	 * @param toAdd new identity
	 * @param credReqId Local {@link CredentialRequirements} id
	 * @param initialCredentialState initial state of the credential. Only {@link LocalAuthenticationState#disabled}
	 * and {@link LocalAuthenticationState#outdated} are allowed.
	 * @return newly created identity
	 * @throws EngineException
	 */
	public Identity addIdentity(IdentityParam toAdd, String credReqIdId, 
			LocalAuthenticationState initialCredentialState) throws EngineException;
	
	/**
	 * Adds a new identity under existing entity.
	 * @param toAdd
	 * @param equivalentIdentity
	 * @return newly created identity
	 * @throws EngineException
	 */
	public Identity addIdentity(IdentityParam toAdd, EntityParam parentEntity) throws EngineException;
	
	/**
	 * Deletes identity. It must not be the last identity of the entity.
	 * Certain system Identities can not be removed.
	 * <p>
	 * @param toRemove
	 * @throws EngineException
	 */
	public void removeIdentity(IdentityTaV toRemove) throws EngineException;

	/**
	 * Deletes entity.
	 * <p>
	 * @param toRemove
	 * @throws EngineException
	 */
	public void removeEntity(EntityParam toRemove) throws EngineException;

	/**
	 * Enables/disables identity
	 * @param toChange
	 * @param status
	 * @throws EngineException
	 */
	public void setIdentityStatus(IdentityTaV toChange, boolean status) 
			throws EngineException;
	
	/**
	 * Returns information about an entity along with its all identities.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Entity getEntity(EntityParam entity) throws EngineException;
	
	/**
	 * Changes {@link CredentialRequirements} of an entity
	 * @param entity to be modified
	 * @param requirementId to be set
	 * @param desiredAuthnState controls how to handle the existing authN material. 
	 * If set to 'correct' then change is applied only if the new requirements are compatible with the previous 
	 * data. In other cases the value is set for the identity after update.
	 * @throws EngineException
	 */
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId, 
			LocalAuthenticationState desiredAuthnState) throws EngineException;
	
	/**
	 * Sets authentication secretes for the entity. 
	 * @param entity to be modified
	 * @param credentialId credential id to be changed. 
	 * @param jsonSecrets the credential type specific value of the credential encoded in JSON
	 * @throws EngineException
	 */
	public void setEntityCredential(EntityParam entity, String credentialId, String jsonSecrets) 
			throws EngineException;
	
}

