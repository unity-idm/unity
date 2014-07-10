/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributesClass;
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
	 * @param initialState the initial state of the newly created entity
	 * @param extractAttributes whether automatic attributes extraction should be performed
	 * @param attributes initial attributes to be added for the entity. This is especially useful 
	 * when the root group (to which the entity is automatically added) has some {@link AttributesClass}es assigned
	 * with mandatory attributes. 
	 * @return newly created identity
	 * @throws EngineException
	 */
	public Identity addEntity(IdentityParam toAdd, String credReqIdId, EntityState initialState,
			boolean extractAttributes, List<Attribute<?>> attributes) throws EngineException;

	/**
	 * As {@link #addEntity(IdentityParam, String, EntityState, boolean, List)} with the empty list of attributes.
	 */
	public Identity addEntity(IdentityParam toAdd, String credReqIdId, EntityState initialState,
			boolean extractAttributes) throws EngineException;
	
	/**
	 * Adds a new identity under existing entity.
	 * @param toAdd
	 * @param equivalentIdentity
	 * @param extractAttributes whether automatic attributes extraction should be performed
	 * @return newly created identity
	 * @throws EngineException
	 */
	public Identity addIdentity(IdentityParam toAdd, EntityParam parentEntity, boolean extractAttributes) 
			throws EngineException;
	
	/**
	 * Deletes identity. It must not be the last identity of the entity.
	 * Certain system Identities can not be removed.
	 * <p>
	 * @param toRemove
	 * @throws EngineException
	 */
	public void removeIdentity(IdentityTaV toRemove) throws EngineException;

	/**
	 * Reset a possibly targeted value of a dynamic identity. For the identities which are fixed this method 
	 * throws an exception. 
	 * <p>
	 * @param entity subject
	 * @param typeIdToReset which id type should be reset, must be dynamic
	 * @param realm if null then all realms should be cleared
	 * @param target if null then all targets should be cleared
	 * @throws EngineException
	 */
	public void resetIdentity(EntityParam entity, String typeIdToReset, 
			String realm, String target) throws EngineException;

	/**
	 * Deletes entity.
	 * <p>
	 * @param toRemove
	 * @throws EngineException
	 */
	public void removeEntity(EntityParam toRemove) throws EngineException;

	/**
	 * Sets entity status
	 * @param toChange
	 * @param state
	 * @throws EngineException
	 */
	public void setEntityStatus(EntityParam toChange, EntityState state) 
			throws EngineException;
	
	/**
	 * Returns information about an entity along with its all identities.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Entity getEntity(EntityParam entity) throws EngineException;

	/**
	 * Returns information about an entity along with its all identities.
	 * This version supports dynamic identities as it allows for specifying a receiver of the information 
	 * and whether it is allowed to establish a new identifier.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Entity getEntity(EntityParam entity, String target, boolean allowCreate) throws EngineException;

	/**
	 * Returns information about an entity along with its identities.
	 * This version requires higher privileges and returns all identities, also those targeted 
	 * for anybody in any realm.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	Entity getEntityNoContext(EntityParam entity) throws EngineException;

	
	/**
	 * Changes {@link CredentialRequirements} of an entity. 
	 * @param entity to be modified
	 * @param requirementId to be set
	 * @throws EngineException
	 */
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId) throws EngineException;
	
	/**
	 * Sets authentication secretes for the entity. After the change, the credential will be in correct state.  
	 * @param entity to be modified
	 * @param credentialId credential id to be changed. 
	 * @param secrets the credential type specific value of the credential. 
	 * @throws EngineException
	 */
	public void setEntityCredential(EntityParam entity, String credentialId, String secrets) throws EngineException;

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
	public void setEntityCredentialStatus(EntityParam entity, String credentialId,  
			LocalCredentialState desiredCredentialState) throws EngineException;
	
	/**
	 * Returns a collection with all groups where the entity is a member.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Collection<String> getGroups(EntityParam entity) throws EngineException;
}

