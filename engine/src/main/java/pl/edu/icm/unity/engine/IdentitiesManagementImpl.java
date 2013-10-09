/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.DBShared;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
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
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Implementation of identities management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * @author K. Benedyczak
 */
@Component
public class IdentitiesManagementImpl implements IdentitiesManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdentitiesManagementImpl.class);
	private DBSessionManager db;
	private DBIdentities dbIdentities;
	private DBGroups dbGroups;
	private DBAttributes dbAttributes;
	private DBShared dbShared;
	private IdentitiesResolver idResolver;
	private EngineHelper engineHelper;
	private AuthorizationManager authz;
	private IdentityTypesRegistry idTypesRegistry;

	@Autowired
	public IdentitiesManagementImpl(DBSessionManager db, DBIdentities dbIdentities,
			DBGroups dbGroups, DBAttributes dbAttributes, DBShared dbShared,
			IdentitiesResolver idResolver, EngineHelper engineHelper,
			AuthorizationManager authz, IdentityTypesRegistry idTypesRegistry)
	{
		this.db = db;
		this.dbIdentities = dbIdentities;
		this.dbGroups = dbGroups;
		this.dbAttributes = dbAttributes;
		this.dbShared = dbShared;
		this.idResolver = idResolver;
		this.engineHelper = engineHelper;
		this.authz = authz;
		this.idTypesRegistry = idTypesRegistry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<IdentityType> getIdentityTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			List<IdentityType> ret = dbIdentities.getIdentityTypes(sqlMap);
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
			engineHelper.checkGroupAttributeClassesConsistency(attributes, "/", sqlMap);
			
			Identity ret = dbIdentities.insertIdentity(toAdd, null, sqlMap);
			long entityId = ret.getEntityId();
			if (!PersistentIdentity.ID.equals(toAdd.getTypeId()))
			{
				IdentityParam persistent = new IdentityParam(PersistentIdentity.ID, 
						PersistentIdentity.getNewId(), true);
				dbIdentities.insertIdentity(persistent, entityId, sqlMap);
			}
			
			dbIdentities.setEntityStatus(entityId, initialState, sqlMap);
			dbGroups.addMemberFromParent("/", new EntityParam(ret.getEntityId()), sqlMap);
			engineHelper.setEntityCredentialRequirements(entityId, credReqId, sqlMap);
			
			engineHelper.addAttributesList(attributes, entityId, sqlMap);
			
			if (extractAttributes)
				extractAttributes(ret, sqlMap);
			
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
	public Identity addIdentity(IdentityParam toAdd, EntityParam parentEntity, boolean extractAttributes)
			throws EngineException
	{
		toAdd.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(parentEntity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			Identity ret = dbIdentities.insertIdentity(toAdd, entityId, sqlMap);
			if (extractAttributes)
				extractAttributes(ret, sqlMap);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	private void extractAttributes(Identity from, SqlSession sql)
	{
		IdentityType idType = from.getType();
		IdentityTypeDefinition typeProvider = idType.getIdentityTypeProvider();
		Map<String, String> toExtract = idType.getExtractedAttributes();
		List<Attribute<?>> extractedList = typeProvider.extractAttributes(from.getValue(), toExtract);
		long entityId = from.getEntityId();
		for (Attribute<?> extracted: extractedList)
		{
			extracted.setGroupPath("/");
			try
			{
				dbAttributes.addAttribute(entityId, extracted, false, sql);
			} catch (EngineException e)
			{
				log.info("Can not add extracted attribute " + extracted.getName() 
						+ " for entity " + entityId + ": " + e.toString());
			}
		}
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
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			dbIdentities.removeIdentity(toRemove, sqlMap);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Entity getEntity(EntityParam entity) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.read);
			Identity[] identities = dbIdentities.getIdentitiesForEntity(entityId, sqlMap);
			CredentialInfo credInfo = getCredentialInfo(entityId, sqlMap);
			EntityState theState = dbIdentities.getEntityStatus(entityId, sqlMap);
			Entity ret = new Entity(entityId, identities, theState, credInfo);
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
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.identityModify);
			engineHelper.setEntityCredentialRequirements(entityId, requirementId, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public void setEntityCredential(EntityParam entity, String credentialId, String rawCredential) throws EngineException
	{
		if (rawCredential == null)
			throw new IllegalCredentialException("The credential can not be null");
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			authz.checkAuthorization(authz.isSelf(entityId), AuthzCapability.credentialModify);
			engineHelper.setEntityCredentialInternal(entityId, credentialId, rawCredential, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
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
				attributes.put(credentialAttributeName, new AttributeExt(newCredentialA, true));
				dbAttributes.addAttribute(entityId, newCredentialA, true, sqlMap);
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
}
