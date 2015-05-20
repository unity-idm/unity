/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.DBShared;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Implementation of identities management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * @author K. Benedyczak
 */
@Component
public class IdentitiesManagementImpl implements IdentitiesManagement
{
	private DBSessionManager db;
	private DBIdentities dbIdentities;
	private DBAttributes dbAttributes;
	private DBShared dbShared;
	private IdentitiesResolver idResolver;
	private EngineHelper engineHelper;
	private AuthorizationManager authz;
	private IdentityTypesRegistry idTypesRegistry;
	private ConfirmationManager confirmationManager;
	
	@Autowired
	public IdentitiesManagementImpl(DBSessionManager db, DBIdentities dbIdentities,
			DBAttributes dbAttributes, DBShared dbShared,
			IdentitiesResolver idResolver, EngineHelper engineHelper, AttributesHelper attributesHelper,
			AuthorizationManager authz, IdentityTypesRegistry idTypesRegistry,
			ConfirmationManager confirmationsManager)
	{
		this.db = db;
		this.dbIdentities = dbIdentities;
		this.dbAttributes = dbAttributes;
		this.dbShared = dbShared;
		this.idResolver = idResolver;
		this.engineHelper = engineHelper;
		this.authz = authz;
		this.idTypesRegistry = idTypesRegistry;
		this.confirmationManager = confirmationsManager;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<IdentityType> getIdentityTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			Collection<IdentityType> ret = dbIdentities.getIdentityTypes(sqlMap).values();
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateIdentityType(IdentityType toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toUpdate.getIdentityTypeProvider().getId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			if (toUpdate.getMinInstances() < 0)
				throw new IllegalAttributeTypeException("Minimum number of instances "
						+ "can not be negative");
			if (toUpdate.getMinVerifiedInstances() > toUpdate.getMinInstances())
				throw new IllegalAttributeTypeException("Minimum number of verified instances "
						+ "can not be larger then the regular minimum of instances");
			if (toUpdate.getMinInstances() > toUpdate.getMaxInstances())
				throw new IllegalAttributeTypeException("Minimum number of instances "
						+ "can not be larger then the maximum");
			
			
			Map<String, AttributeType> atsMap = dbAttributes.getAttributeTypes(sqlMap);
			Map<String, String> extractedAts = toUpdate.getExtractedAttributes();
			Set<AttributeType> supportedForExtraction = idTypeDef.getAttributesSupportedForExtraction();
			Map<String, AttributeType> supportedForExtractionMap = new HashMap<String, AttributeType>();
			for (AttributeType at: supportedForExtraction)
				supportedForExtractionMap.put(at.getName(), at);
			
			for (Map.Entry<String, String> extracted: extractedAts.entrySet())
			{
				AttributeType type = atsMap.get(extracted.getValue());
				if (type == null)
					throw new IllegalAttributeTypeException("Can not extract attribute " + 
							extracted.getKey() + " as " + extracted.getValue() + 
							" because the latter is not defined in the system");
				AttributeType supportedType = supportedForExtractionMap.get(extracted.getKey());
				if (supportedType == null)
					throw new IllegalAttributeTypeException("Can not extract attribute " + 
							extracted.getKey() + " as " + extracted.getValue() + 
							" because the former is not supported by the identity provider");
			}
			dbIdentities.updateIdentityType(sqlMap, toUpdate);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public Identity addEntity(IdentityParam toAdd, String credReqId, EntityState initialState,
			boolean extractAttributes) throws EngineException
	{
		return addEntity(toAdd, credReqId, initialState, extractAttributes, null);
	}

	@Override
	public Identity addEntity(IdentityParam toAdd, String credReqId,
			EntityState initialState, boolean extractAttributes,
			List<Attribute<?>> attributes) throws EngineException
	{
		toAdd.validateInitialization();
		authz.checkAuthorization(AuthzCapability.identityModify);
		if (attributes == null)
			attributes = Collections.emptyList();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			Identity ret = engineHelper.addEntity(toAdd, credReqId, initialState, 
					extractAttributes, attributes, true, sqlMap);
			sqlMap.commit();
			
			//careful - must be after the transaction is committed
			EntityParam added = new EntityParam(ret.getEntityId());
			confirmationManager.sendVerificationsQuiet(added, attributes, false);
			confirmationManager.sendVerificationQuiet(added, ret, false);
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity addIdentity(IdentityParam toAdd, EntityParam parentEntity, boolean extractAttributes)
			throws EngineException
	{
		toAdd.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(parentEntity, sqlMap);
			IdentityType identityType = dbIdentities.getIdentityTypes(sqlMap).get(
					toAdd.getTypeId());
			
			boolean fullAuthz = authorizeIdentityChange(entityId, Sets.newHashSet(toAdd), 
					identityType.isSelfModificable());
			Identity[] identities = dbIdentities.getIdentitiesForEntityNoContext(entityId, sqlMap);
			if (!fullAuthz && getIdentityCountOfType(identities, identityType.getIdentityTypeProvider().getId()) 
					>= identityType.getMaxInstances())
				throw new SchemaConsistencyException("Can not add another identity of this type as "
						+ "the configured maximum number of instances was reached.");
			Identity ret = dbIdentities.insertIdentity(toAdd, entityId, false, sqlMap);
			if (extractAttributes && fullAuthz)
				engineHelper.extractAttributes(ret, sqlMap);
			sqlMap.commit();
			confirmationManager.sendVerification(new EntityParam(entityId), ret, false);
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	private int getIdentityCountOfType(Identity[] identities, String type)
	{
		int ret = 0;
		for (Identity id: identities)
			if (id.getTypeId().equals(type))
				ret++;
		return ret;
	}

	private void checkVerifiedMinCountForRemoval(Identity[] identities, IdentityTaV toRemove,
			IdentityType type) throws SchemaConsistencyException, IllegalIdentityValueException
	{
		if (!type.getIdentityTypeProvider().isVerifiable())
			return;
		int existing = 0;
		String comparableValue = type.getIdentityTypeProvider().getComparableValue(toRemove.getValue(), 
				toRemove.getRealm(), toRemove.getTarget());
		for (Identity id: identities)
		{
			if (id.getTypeId().equals(toRemove.getTypeId()))
			{
				if (comparableValue.equals(id.getComparableValue()) && !id.isConfirmed())
					return;
				if (id.isConfirmed())
					existing++;
			}
		}
		if (existing <= type.getMinVerifiedInstances())
			throw new SchemaConsistencyException("Can not remove the verified identity as "
					+ "the configured minimum number of verified instances was reached.");
	}
	
	/**
	 * Checks if identityModify capability is granted. If it is only in self access context the
	 * confirmation status is forced to be unconfirmed.
	 * @throws AuthorizationException
	 * @returns true if full authZ is set or false if limited only. 
	 */
	private boolean authorizeIdentityChange(long entityId, Collection<? extends IdentityParam> toAdd, 
			boolean selfModifiable) throws AuthorizationException
	{
		boolean fullAuthz = authz.getCapabilities(false, "/").contains(AuthzCapability.identityModify);
		if (!fullAuthz)
		{
			authz.checkAuthorization(selfModifiable && authz.isSelf(entityId), 
					AuthzCapability.identityModify);
			for (IdentityParam idP: toAdd)
				idP.setConfirmationInfo(new ConfirmationInfo());
			return false;
		}
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeIdentity(IdentityTaV toRemove) throws EngineException
	{
		toRemove.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(new EntityParam(toRemove), sqlMap);
			IdentityType identityType = dbIdentities.getIdentityTypes(sqlMap).get(
					toRemove.getTypeId());
			Identity[] identities = dbIdentities.getIdentitiesForEntityNoContext(entityId, sqlMap);
			String type = identityType.getIdentityTypeProvider().getId();
			boolean fullAuthz = authorizeIdentityChange(entityId, new ArrayList<IdentityParam>(), 
					identityType.isSelfModificable());
			if (!fullAuthz)
			{
				if (getIdentityCountOfType(identities, type) <= identityType.getMinInstances())
					throw new SchemaConsistencyException("Can not remove the identity as "
						+ "the configured minimum number of instances was reached.");
				checkVerifiedMinCountForRemoval(identities, toRemove, identityType);
			}
			dbIdentities.removeIdentity(toRemove, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}


	@Override
	public void setIdentities(EntityParam entity, Collection<String> updatedTypes,
			Collection<? extends IdentityParam> newIdentities) throws EngineException
	{
		entity.validateInitialization();
		ensureNoDynamicIdentityType(updatedTypes);
		ensureIdentitiesAreOfSpecifiedTypes(updatedTypes, newIdentities);
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			Map<String, IdentityType> identityTypes = dbIdentities.getIdentityTypes(sqlMap);
			boolean selfModifiable = areAllTypesSelfModifiable(updatedTypes, identityTypes);
			boolean fullAuthz = authorizeIdentityChange(entityId, newIdentities, selfModifiable);
			Identity[] identities = dbIdentities.getIdentitiesForEntityNoContext(entityId, sqlMap);
			Map<String, Set<Identity>> currentIdentitiesByType = 
					getCurrentIdentitiesByType(updatedTypes, identities);
			Map<String, Set<IdentityParam>> requestedIdentitiesByType = 
					getRequestedIdentitiesByType(updatedTypes, newIdentities);
			for (String type: updatedTypes)
				setIdentitiesOfType(identityTypes.get(type), entityId, currentIdentitiesByType.get(type), 
						requestedIdentitiesByType.get(type), fullAuthz, sqlMap);			
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	private void setIdentitiesOfType(IdentityType type, long entityId, 
			Set<Identity> existing, Set<IdentityParam> requested, boolean fullAuthz,
			SqlSession sqlMap) throws EngineException
	{
		Set<IdentityParam> toRemove = substractIdentitySets(type, existing, requested);
		Set<IdentityParam> toAdd = substractIdentitySets(type, requested, existing);
		verifyLimitsOfIdentities(type, existing, requested, toRemove, toAdd, fullAuthz);
		
		for (IdentityParam add: toAdd)
			dbIdentities.insertIdentity(add, entityId, false, sqlMap);
		for (IdentityParam remove: toRemove)
			dbIdentities.removeIdentity(remove, sqlMap);
	}
	
	private void verifyLimitsOfIdentities(IdentityType type, Set<Identity> existing, Set<IdentityParam> requested, 
			Set<IdentityParam> toRemove, Set<IdentityParam> toAdd, boolean fullAuthz) 
			throws SchemaConsistencyException
	{
		if (fullAuthz)
			return;
		
		int newCount = requested.size();
		if (newCount < type.getMinInstances() && existing.size() >= type.getMaxInstances())
			throw new SchemaConsistencyException("The operation can not be completed as in effect "
					+ "the configured minimum number of instances would be violated "
					+ "for the identity type " + type.getIdentityTypeProvider().getId());
		if (newCount > type.getMaxInstances() && existing.size() <= type.getMaxInstances())
			throw new SchemaConsistencyException("The operation can not be completed as in effect "
					+ "the configured maximum number of instances would be violated "
					+ "for the identity type " + type.getIdentityTypeProvider().getId());
		if (type.getIdentityTypeProvider().isVerifiable())
		{
			int newConfirmedCount = 0;
			int currentConfirmedCount = 0;
			for (IdentityParam ni: existing)
				if (ni.isConfirmed())
					newConfirmedCount++;
			currentConfirmedCount = newConfirmedCount;
			for (IdentityParam ni: toRemove)
				if (ni.isConfirmed())
					newConfirmedCount--;
			for (IdentityParam ni: toAdd)
				if (ni.isConfirmed())
					newConfirmedCount++;
			
			if (newConfirmedCount < type.getMinVerifiedInstances() && 
					currentConfirmedCount >= type.getMinVerifiedInstances())
				throw new SchemaConsistencyException("The operation can not be completed as in effect "
					+ "the configured minimum number of confirmed identities would be violated "
					+ "for the identity type " + type.getIdentityTypeProvider().getId());
		}
		
	}
	
	private Set<IdentityParam> substractIdentitySets(IdentityType type, Set<? extends IdentityParam> from, 
			Set<? extends IdentityParam> what) throws IllegalIdentityValueException
	{
		IdentityTypeDefinition identityTypeProvider = type.getIdentityTypeProvider();
		Set<IdentityParam> ret = new HashSet<>();
		for (IdentityParam idParam: from)
		{
			String idParamCmp = identityTypeProvider.getComparableValue(idParam.getValue(), 
					idParam.getRealm(), idParam.getTarget());
			boolean found = false;
			for (IdentityParam removed: what)
			{
				String removedCmp = identityTypeProvider.getComparableValue(removed.getValue(), 
						removed.getRealm(), removed.getTarget());
				
				if (idParamCmp.equals(removedCmp))
				{
					found = true;
					break;
				}
			}
			if (!found)
				ret.add(idParam);
		}
		return ret;
	}
	

	private Map<String, Set<IdentityParam>> getRequestedIdentitiesByType(Collection<String> updatedTypes, 
			Collection<? extends IdentityParam> identities)
	{
		Map<String, Set<IdentityParam>> ret = new HashMap<>();
		for (String type: updatedTypes)
			ret.put(type, new HashSet<IdentityParam>());
		for (IdentityParam id: identities)
		{
			ret.get(id.getTypeId()).add(id);
		}
		return ret;
	}
	
	private Map<String, Set<Identity>> getCurrentIdentitiesByType(Collection<String> updatedTypes, 
			Identity[] identities)
	{
		Map<String, Set<Identity>> ret = new HashMap<>();
		for (String type: updatedTypes)
			ret.put(type, new HashSet<Identity>());
		for (Identity id: identities)
		{
			if (!updatedTypes.contains(id.getTypeId()))
				continue;
			ret.get(id.getTypeId()).add(id);
		}
		return ret;
	}
	
	private boolean areAllTypesSelfModifiable(Collection<String> updatedTypes, 
			Map<String, IdentityType> identityTypes)
	{
		for (String type: updatedTypes)
		{
			IdentityType idType = identityTypes.get(type);
			if (!idType.isSelfModificable())
			{
				return false;
			}
		}
		return true;
	}
	
	private void ensureIdentitiesAreOfSpecifiedTypes(Collection<String> updatedTypes,
			Collection<? extends IdentityParam> newIdentities) throws IllegalIdentityValueException
	{
		for (IdentityParam id: newIdentities)
		{
			id.validateInitialization();
			if (!updatedTypes.contains(id.getTypeId()))
				throw new IllegalArgumentException("All new identities must be "
						+ "of types specified as the first argument");
			
		}		
	}
	
	private void ensureNoDynamicIdentityType(Collection<String> updatedTypes) 
			throws IllegalTypeException, IllegalIdentityValueException
	{
		for (String type: updatedTypes)
		{
			IdentityTypeDefinition idType = idTypesRegistry.getByName(type);
			if (idType.isDynamic())
				throw new IllegalIdentityValueException("Identity type " + type + 
						" is dynamic and can not be manually set");

		}		
	}
	
	
	@Override
	public void resetIdentity(EntityParam toReset, String typeIdToReset,
			String realm, String target) throws EngineException
	{
		toReset.validateInitialization();
		if (typeIdToReset == null)
			throw new IllegalIdentityValueException("Identity type can not be null");
		IdentityTypeDefinition idType = idTypesRegistry.getByName(typeIdToReset);
		if (!idType.isDynamic())
			throw new IllegalIdentityValueException("Identity type " + typeIdToReset + 
					" is not dynamic and can not be reset");
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toReset, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.resetIdentityForEntity(entityId, typeIdToReset, realm, target, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeEntity(EntityParam toRemove) throws EngineException
	{
		toRemove.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toRemove, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.removeEntity(entityId, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEntityStatus(EntityParam toChange, EntityState status)
			throws EngineException
	{
		toChange.validateInitialization();
		if (status == EntityState.onlyLoginPermitted)
			throw new IllegalArgumentException("The new entity status 'only login permitted' "
					+ "can be only set as a side effect of scheduling an account "
					+ "removal with a grace period.");
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toChange, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.setEntityStatus(entityId, status, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public Entity getEntity(EntityParam entity) throws EngineException
	{
		return getEntity(entity, null, true, "/");
	}
	
	@Override
	public Entity getEntityNoContext(EntityParam entity, String group) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			Entity ret;
			try
			{
				authz.checkAuthorization(authz.isSelf(entityId), group, AuthzCapability.readHidden);
				Identity[] identities = dbIdentities.getIdentitiesForEntityNoContext(entityId, sqlMap);
				ret = assembleEntity(entityId, identities, sqlMap);
			} catch (AuthorizationException e)
			{
				ret = resolveEntityBasic(entityId, null, false, group, sqlMap);
			}
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	@Override
	public Entity getEntity(EntityParam entity, String target, boolean allowCreate, String group)
			throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			Entity ret = resolveEntityBasic(entityId, target, allowCreate, group, sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	/**
	 * Checks if read cap is set and resolved the entity: identities and credential with respect to the
	 * given target.
	 * @param entityId
	 * @param target
	 * @param allowCreate
	 * @param sqlMap
	 * @return
	 * @throws EngineException
	 */
	private Entity resolveEntityBasic(long entityId, String target, boolean allowCreate, String group, 
			SqlSession sqlMap) throws EngineException
	{
		authz.checkAuthorization(authz.isSelf(entityId), group, AuthzCapability.read);
		Identity[] identities = dbIdentities.getIdentitiesForEntity(entityId, target, allowCreate, 
				sqlMap);
		return assembleEntity(entityId, identities, sqlMap);
	}
	
	/**
	 * assembles the final entity by adding the credential and state info.
	 * @param entityId
	 * @param identities
	 * @param sqlMap
	 * @return
	 * @throws EngineException
	 */
	private Entity assembleEntity(long entityId, Identity[] identities, SqlSession sqlMap) throws EngineException
	{
		CredentialInfo credInfo = getCredentialInfo(entityId, sqlMap);
		EntityInformation theState = dbIdentities.getEntityInformation(entityId, sqlMap);
		return new Entity(entityId, identities, theState, credInfo);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getGroups(EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
			Set<String> allGroups = dbShared.getAllGroups(entityId, sqlMap);
			sqlMap.commit();
			return allGroups;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public Collection<Group> getGroupsForPresentation(EntityParam entity)
			throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
			Set<Group> ret = dbShared.getAllGroupsWithNames(entityId, sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	@Override
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			engineHelper.setEntityCredentialRequirements(entityId, requirementId, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public boolean isCurrentCredentialRequiredForChange(EntityParam entity, String credentialId)
			throws EngineException
	{
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			boolean ret = authorizeCredentialChange(entityId, credentialId, sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}

	}
	
	@Override
	public void setEntityCredential(EntityParam entity, String credentialId, String rawCredential) 
			throws EngineException
	{
		setEntityCredential(entity, credentialId, rawCredential, null);
	}
	
	@Override
	public void setEntityCredential(EntityParam entity, String credentialId, String rawCredential,
			String currentRawCredential) throws EngineException
	{
		if (rawCredential == null)
			throw new IllegalCredentialException("The credential can not be null");
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			boolean requireCurrent = authorizeCredentialChange(entityId, credentialId, sqlMap);

			if (requireCurrent && currentRawCredential == null)
			{
				throw new IllegalPreviousCredentialException(
						"The current credential must be provided");
			}
			
			//we don't check it 
			if (!requireCurrent)
				currentRawCredential = null;
			
			engineHelper.setEntityCredentialInternal(entityId, credentialId, rawCredential, 
					currentRawCredential, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	/**
	 * Performs authorization of attribute change. The method also returns whether a current credential is 
	 * required to change the previous one, what is needed if the current credential is set and the caller 
	 * doesn't have the credentialModify capability set globally.
	 * @param entityId
	 * @param credentialId
	 * @return
	 * @throws EngineException 
	 */
	private boolean authorizeCredentialChange(long entityId, String credentialId, SqlSession sqlMap) 
			throws EngineException
	{
		try
		{
			authz.checkAuthorization(AuthzCapability.credentialModify);
			return false;
		} catch (AuthorizationException e)
		{
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.credentialModify);
		}
		
		//possible OPTIMIZATION: can get the status of selected credential only 
		CredentialInfo credsInfo = getCredentialInfo(entityId, sqlMap);
		CredentialPublicInformation credInfo = credsInfo.getCredentialsState().get(credentialId);
		if (credInfo == null)
			throw new IllegalCredentialException("The credential " + credentialId + 
					" is not allowed for the entity");
		
		if (credInfo.getState() == LocalCredentialState.notSet)
			return false;
		return true;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void setEntityCredentialStatus(EntityParam entity, String credentialId,
			LocalCredentialState desiredCredentialState) throws EngineException
	{
		entity.validateInitialization();
		if (desiredCredentialState == LocalCredentialState.correct)
			throw new WrongArgumentException("Credential can not be put into the correct state with this method. Use setEntityCredential.");
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(
					entityId, "/", null, sqlMap);
			
			Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
			String credentialRequirements = (String)credReqA.getValues().get(0);
			CredentialRequirementsHolder credReqs = engineHelper.getCredentialRequirements(
					credentialRequirements, sqlMap);
			LocalCredentialVerificator handler = credReqs.getCredentialHandler(credentialId);
			if (handler == null)
				throw new IllegalCredentialException("The credential id is not among the entity's credential requirements: " + credentialId);

			String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialId;
			Attribute<?> currentCredentialA = attributes.get(credentialAttributeName);
			String currentCredential = currentCredentialA != null ? 
					(String)currentCredentialA.getValues().get(0) : null;
					
			if (currentCredential == null)
			{ 
				if (desiredCredentialState != LocalCredentialState.notSet)
					throw new IllegalCredentialException("The credential is not set, so it's state can be only notSet");
				return;
			}
			
			//remove or invalidate
			if (desiredCredentialState == LocalCredentialState.notSet)
			{
				dbAttributes.removeAttribute(entityId, "/", credentialAttributeName, sqlMap);
				attributes.remove(credentialAttributeName);
			} else if (desiredCredentialState == LocalCredentialState.outdated)
			{
				if (!handler.isSupportingInvalidation())
					throw new IllegalCredentialException("The credential doesn't support the outdated state");
				String updated = handler.invalidate(currentCredential);
				StringAttribute newCredentialA = new StringAttribute(credentialAttributeName, 
						"/", AttributeVisibility.local, Collections.singletonList(updated));
				Date now = new Date();
				AttributeExt added = new AttributeExt(newCredentialA, true, now, now);
				attributes.put(credentialAttributeName, added);
				dbAttributes.addAttribute(entityId, added, true, sqlMap);
			}
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	private CredentialInfo getCredentialInfo(long entityId, SqlSession sqlMap) 
			throws EngineException
	{
		Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(entityId, "/", null, sqlMap);
		
		Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
		if (credReqA == null)
			throw new InternalException("No credential requirement set for an entity"); 
		String credentialRequirementId = (String)credReqA.getValues().get(0);
		
		CredentialRequirementsHolder credReq = engineHelper.getCredentialRequirements(
				credentialRequirementId, sqlMap);
		Set<String> required = credReq.getCredentialRequirements().getRequiredCredentials();
		Map<String, CredentialPublicInformation> credentialsState = new HashMap<>();
		for (String cd: required)
		{
			LocalCredentialVerificator handler = credReq.getCredentialHandler(cd);
			Attribute<?> currentCredA = attributes.get(SystemAttributeTypes.CREDENTIAL_PREFIX+cd);
			String currentCred = currentCredA == null ? null : (String)currentCredA.getValues().get(0);
			
			credentialsState.put(cd, handler.checkCredentialState(currentCred));
		}
		
		return new CredentialInfo(credentialRequirementId, credentialsState);
	}
	

	@Override
	public void scheduleEntityChange(EntityParam toChange, Date changeTime,
			EntityScheduledOperation operation) throws EngineException
	{
		toChange.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toChange, sqlMap);

			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			
			if (changeTime.getTime() <= System.currentTimeMillis())
				dbIdentities.performScheduledOperation(entityId, operation, sqlMap);
			else
				dbIdentities.setScheduledOperationByAdmin(entityId, changeTime, operation, sqlMap);
			
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public void scheduleRemovalByUser(EntityParam toChange, Date changeTime)
			throws EngineException
	{
		toChange.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(toChange, sqlMap);

			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.attributeModify);
			
			if (changeTime.getTime() <= System.currentTimeMillis())
				dbIdentities.performScheduledOperation(entityId, 
						EntityScheduledOperation.REMOVE, sqlMap);
			else
				dbIdentities.setScheduledRemovalByUser(entityId, changeTime, sqlMap);
			
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}		
	}

}
