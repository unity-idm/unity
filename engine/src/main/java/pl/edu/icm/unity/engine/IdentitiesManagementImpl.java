/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.authn.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.internal.EngineHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.impl.identity.PersistentIdentity;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.LocalCredentialHandler;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;

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
	private DBGroups dbGroups;
	private DBAttributes dbAttributes;
	private IdentitiesResolver idResolver;
	private EngineHelper engineHelper;

	@Autowired
	public IdentitiesManagementImpl(DBSessionManager db, DBIdentities dbIdentities,
			DBGroups dbGroups, DBAttributes dbAttributes, 
			IdentitiesResolver idResolver, EngineHelper engineHelper)
	{
		super();
		this.db = db;
		this.dbIdentities = dbIdentities;
		this.dbGroups = dbGroups;
		this.dbAttributes = dbAttributes;
		this.idResolver = idResolver;
		this.engineHelper = engineHelper;
	}



	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<IdentityType> getIdentityTypes() throws EngineException
	{
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
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity addIdentity(IdentityParam toAdd, String credReqId, 
			LocalAuthenticationState initialCredentialState) throws EngineException
	{
		toAdd.validateInitialization();
		if (initialCredentialState == LocalAuthenticationState.valid)
			throw new IllegalArgumentException("Can not set 'valid' credential state for a new identity," +
					"without any credential defined");
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			Identity ret = dbIdentities.insertIdentity(toAdd, null, sqlMap);
			long entityId = Long.parseLong(ret.getEntityId());
			if (!PersistentIdentity.ID.equals(toAdd.getTypeId()))
			{
				IdentityParam persistent = new IdentityParam(PersistentIdentity.ID, 
						PersistentIdentity.getNewId(),
						toAdd.isEnabled(), true);
				dbIdentities.insertIdentity(persistent, entityId, sqlMap);
			}
			
			dbGroups.addMemberFromParent("/", new EntityParam(ret.getEntityId()), sqlMap);

			engineHelper.setEntityCredentialRequirements(entityId, credReqId, sqlMap);
			engineHelper.setEntityAuthenticationState(entityId, initialCredentialState, sqlMap);
			
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
	public Identity addIdentity(IdentityParam toAdd, EntityParam parentEntity)
			throws EngineException
	{
		toAdd.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(parentEntity, sqlMap);
			Identity ret = dbIdentities.insertIdentity(toAdd, entityId, sqlMap);
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
	public void removeIdentity(IdentityTaV toRemove) throws EngineException
	{
		toRemove.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
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
			dbIdentities.removeEntity(toRemove, sqlMap);
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
	public void setIdentityStatus(IdentityTaV toChange, boolean status)
			throws EngineException
	{
		toChange.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			dbIdentities.setIdentityStatus(toChange, status, sqlMap);
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
			Identity[] identities = dbIdentities.getIdentitiesForEntity(entityId, sqlMap);
			CredentialInfo credInfo = getCredentialInfo(entityId, sqlMap);
			Entity ret = new Entity(entityId+"", identities, credInfo);
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId,
			LocalAuthenticationState desiredAuthnState) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			if (desiredAuthnState == LocalAuthenticationState.valid)
			{
				CredentialRequirementsHolder newCredReqs = engineHelper.getCredentialRequirements(
						requirementId, sqlMap);
				Map<String, Attribute<?>> attributes = dbAttributes.getAllAttributesAsMap(
						entityId, "/", null, sqlMap);
				if (!newCredReqs.areAllCredentialsValid(attributes))
					throw new IllegalCredentialException("Some of the credentials won't " +
							"be valid after the requirements change. " +
							"The new authentication state can not be set to 'valid'.");
			}
			engineHelper.setEntityCredentialRequirements(entityId, requirementId, sqlMap);
			engineHelper.setEntityAuthenticationState(entityId, desiredAuthnState, sqlMap);
			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public void setEntityCredential(EntityParam entity, String credentialId,
			String rawCredential) throws EngineException
	{
		entity.validateInitialization();
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			long entityId = idResolver.getEntityId(entity, sqlMap);
			Map<String, Attribute<?>> attributes = dbAttributes.getAllAttributesAsMap(entityId, "/", null, sqlMap);
			
			Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
			String credentialRequirements = (String)credReqA.getValues().get(0);
			CredentialRequirementsHolder credReqs = engineHelper.getCredentialRequirements(
					credentialRequirements, sqlMap);
			LocalCredentialHandler verificator = credReqs.getVerificator(credentialId);
			if (verificator == null)
				throw new IllegalCredentialException("The credential id is not among the entity's credential requirements: " + credentialId);

			String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialId;
			Attribute<?> currentCredentialA = attributes.get(credentialAttributeName);
			String currentCredential = currentCredentialA != null ? 
					(String)currentCredentialA.getValues().get(0) : null;
			String newCred = verificator.prepareCredential(rawCredential, currentCredential);
			StringAttribute newCredentialA = new StringAttribute(credentialAttributeName, 
					"/", AttributeVisibility.local, Collections.singletonList(newCred));
			attributes.put(credentialAttributeName, newCredentialA);
			
			dbAttributes.addAttribute(entityId, newCredentialA, true, sqlMap);

			Attribute<?> stateAttributes = attributes.get(SystemAttributeTypes.CREDENTIALS_STATE);
			String credentialStateStr = (String)stateAttributes.getValues().get(0);
			LocalAuthenticationState credentialsState = LocalAuthenticationState.valueOf(credentialStateStr);
			if (credentialsState == LocalAuthenticationState.outdated && 
					credReqs.areAllCredentialsValid(attributes))
			{
				engineHelper.setEntityAuthenticationState(entityId, 
						LocalAuthenticationState.valid, sqlMap);
			}

			sqlMap.commit();
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	
	private CredentialInfo getCredentialInfo(long entityId, SqlSession sqlMap)
	{
		Map<String, Attribute<?>> attributes = dbAttributes.getAllAttributesAsMap(entityId, "/", null, sqlMap);
		
		Attribute<?> credReqA = attributes.get(SystemAttributeTypes.CREDENTIAL_REQUIREMENTS);
		if (credReqA == null)
			throw new RuntimeEngineException("No credential requirement set for an entity"); 
		String credentialRequirementId = (String)credReqA.getValues().get(0);
		
		Attribute<?> authnStateA = attributes.get(SystemAttributeTypes.CREDENTIALS_STATE);
		if (authnStateA == null)
			throw new RuntimeEngineException("No authentication state set for an entity");
		LocalAuthenticationState authenticationState = LocalAuthenticationState.valueOf(
				(String)authnStateA.getValues().get(0));
		
		CredentialRequirementsHolder credReq = engineHelper.getCredentialRequirements(
				credentialRequirementId, sqlMap);
		Set<CredentialDefinition> required = credReq.getCredentialRequirements().getRequiredCredentials();
		Map<String, LocalCredentialState> credentialsState = new HashMap<String, LocalCredentialState>();
		for (CredentialDefinition cd: required)
		{
			LocalCredentialHandler handler = credReq.getVerificator(cd.getName());
			Attribute<?> currentCredA = attributes.get(SystemAttributeTypes.CREDENTIAL_PREFIX+cd.getName());
			String currentCred = currentCredA == null ? null : (String)currentCredA.getValues().get(0);
			credentialsState.put(cd.getName(), handler.checkCredentialState(currentCred));
		}
		
		return new CredentialInfo(credentialRequirementId, 
				authenticationState, 
				credentialsState);
	}
}
