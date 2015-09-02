/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
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
	public Collection<IdentityType> getIdentityTypes() throws EngineException;
	
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
	 * Updates identities of a single entity. The identities of the types provided with the first argument are 
	 * replaced with the identities given as the second argument. The set of new identities can contain only
	 * identities of types enumerated in the first argument (which can have more types, if some needs to be 
	 * cleared). Certain system identity types can not be modified using this method, only the 
	 * {@link #resetIdentity(EntityParam, String, String, String)} is available for them.
	 * @param entity all identities must belong to this entity  
	 * @param updatedTypes set of all types that shall be modified
	 * @param newIdentities a new, complete set of identities for the given types 
	 * @throws EngineException
	 */
	public void setIdentities(EntityParam entity, 
			Collection<String> updatedTypes, Collection<? extends IdentityParam> newIdentities) 
			throws EngineException;

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
	 * Schedules an operation to be invoked at a given time on an entity. 
	 * Requires regular identityModify capability (not assigned for self access).
	 * 
	 * @param toChange
	 * @param changeTime
	 * @param operation
	 * @throws EngineException
	 */
	public void scheduleEntityChange(EntityParam toChange, Date changeTime, EntityScheduledOperation operation) 
			throws EngineException;
	
	/**
	 * Sets the entity in the {@link EntityState#onlyLoginPermitted} and schedules the entity removal at given 
	 * time unless the user logs in before this time. 
	 * Requires only the attributeModify capability (allowed for selfAccess).
	 * 
	 * @param toChange
	 * @param changeTime
	 * @throws EngineException
	 */
	public void scheduleRemovalByUser(EntityParam toChange, Date changeTime) throws EngineException;	
	
	/**
	 * Returns information about an entity along with its all identities with authorization in '/'.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Entity getEntity(EntityParam entity) throws EngineException;

	/**
	 * Returns information about an entity along with its all identities.
	 * This version supports dynamic identities as it allows for specifying a receiver of the information 
	 * and whether it is allowed to establish a new identifier.
	 * @param entity who to resolve
	 * @param target for whom the information is targeted
	 * @param allowCreate whether dynamic identities can be created
	 * @param group group wrt which authorization should be performed.
	 * @return 
	 * @throws EngineException
	 */
	public Entity getEntity(EntityParam entity, String target, boolean allowCreate, String group) throws EngineException;

	/**
	 * Returns information about an entity along with its identities.
	 * This version requires higher privileges and returns all identities, also those targeted 
	 * for anybody in any realm.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	Entity getEntityNoContext(EntityParam entity, String group) throws EngineException;

	
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
	 * @param previousSecrets used to check if the previous credential is known to the caller. 
	 * @throws EngineException
	 */
	public void setEntityCredential(EntityParam entity, String credentialId, String secrets,
			String previousSecrets) throws EngineException;

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
	 * Returns a collection with all groups where the entity is a member. For convenience returned 
	 * as map indexed with group paths.
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Map<String, GroupMembership> getGroups(EntityParam entity) throws EngineException;

	/**
	 * Returns a collection with all groups where the entity is a member. This method 
	 * returns resolved groups with description and displayed name, however without information 
	 * on attribute statements and other data which might be secret.
	 * 
	 * @param entity
	 * @return
	 * @throws EngineException
	 */
	public Collection<Group> getGroupsForPresentation(EntityParam entity) throws EngineException;

	/**
	 * As {@link #setEntityCredential(EntityParam, String, String, String)}
	 * but works only when invoked with super administrative role, so that the 
	 * current credential is not used. 
	 * @param entity
	 * @param credentialId
	 * @param rawCredential
	 * @throws EngineException
	 */
	void setEntityCredential(EntityParam entity, String credentialId, String rawCredential)
			throws EngineException;

	/**
	 * @param entity
	 * @param credentialId
	 * @return true only if the {@link #setEntityCredential(EntityParam, String, String)} method can 
	 * be used. If false then the {@link #setEntityCredential(EntityParam, String, String, String)}
	 * version must be used.
	 * @throws EngineException
	 */
	boolean isCurrentCredentialRequiredForChange(EntityParam entity, String credentialId)
			throws EngineException;

	/**
	 * Merge the 2nd entity with the first one. Only non conflicting information is moved. In particular
	 * the information of the merged entity is processed as follows:
	 * <ol>
	 * <li> static identities are added to the base entity
	 * <li> dynamic identities are added to the base entity only if it has no identities of 
	 * a particular identity type and the identity is removable
	 * <li> credential requirement and attribute classes are ignored
	 * <li> credentials are added, however only if the target identity has no credential defined of the same type
	 * <li> group memberships are copied
	 * <li> attributes are copied, however only if the target has no attribute of the same type in the same group.
	 * Also attributes not allowed by the target's attribute policy are ignored 
	 * </ol>
	 * If the safe mode is activated then the operation will throw exception (without making any changes) 
	 * if any of attributes, credentials or dynamic removable identity was not copied due to conflicts.  
	 * 
	 * @param target
	 * @param merged
	 * @throws EngineException
	 */
	void mergeEntities(EntityParam target, EntityParam merged, boolean safeMode) throws EngineException;
}

