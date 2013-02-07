/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.AuthenticationSecret;
import pl.edu.icm.unity.types.Entity;
import pl.edu.icm.unity.types.EntityLAC;
import pl.edu.icm.unity.types.EntityParam;
import pl.edu.icm.unity.types.Identity;
import pl.edu.icm.unity.types.IdentityParam;
import pl.edu.icm.unity.types.IdentityTaV;
import pl.edu.icm.unity.types.IdentityType;
import pl.edu.icm.unity.types.LocalAccessClass;
import pl.edu.icm.unity.types.LocalAuthnState;

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
	 * @param lacId Local authentication class id to be set for the entity
	 * @return newly created identity
	 * @throws EngineException
	 */
	public Identity addIdentity(IdentityTaV toAdd, String lacId) throws EngineException;
	
	/**
	 * Adds a new identity under existing entity.
	 * @param toAdd
	 * @param equivalentIdentity
	 * @return newly created identity
	 * @throws EngineException
	 */
	public Identity addIdentity(IdentityTaV toAdd, EntityParam parentEntity) throws EngineException;
	
	/**
	 * Deletes identity.
	 * <p>
	 * @param toRemove
	 * @throws EngineException
	 */
	public void removeIdentity(IdentityParam toRemove) throws EngineException;

	/**
	 * Enables/disables identity
	 * @param toChange
	 * @param status
	 * @throws EngineException
	 */
	public void setIdentityStatus(IdentityParam toChange, boolean status) 
			throws EngineException;
	
	/**
	 * Returns information about an entity along with its all identities.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Entity getEntity(EntityParam entity) throws EngineException;
	
	/**
	 * Changes {@link LocalAccessClass} (LAC) of an entity
	 * @param entity to be modified
	 * @param lacId new LAC id
	 * @param desiredAuthnState controls how to handle the existing authN material. 
	 * If set to 'correct' then change is applied only if the new LAC is compatible with the previous 
	 * data. In other cases the value is set for the identity after update.
	 * @throws EngineException
	 */
	public void setEntityLAC(EntityParam entity, String lacId, LocalAuthnState desiredAuthnState) 
			throws EngineException;
	
	/**
	 * Returns LAC of an entity
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public EntityLAC getEntityLAC(EntityParam entity) throws EngineException;
	
	/**
	 * Sets authentication secretes for the entity. 
	 * @param entity
	 * @param secrets
	 * @throws EngineException
	 */
	public void setEntityAuthenticationSecrets(EntityParam entity, List<AuthenticationSecret> secrets) 
			throws EngineException;
	
}

