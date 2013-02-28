/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.impl.identity.PersistentIdentity;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
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
	private IdentitiesResolver idResolver;

	@Autowired
	public IdentitiesManagementImpl(DBSessionManager db, DBIdentities dbIdentities, DBGroups dbGroups, 
			IdentitiesResolver idResolver)
	{
		this.db = db;
		this.dbIdentities = dbIdentities;
		this.dbGroups = dbGroups;
		this.idResolver= idResolver; 
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
	public Identity addIdentity(IdentityParam toAdd, String lacId) throws EngineException
	{
		toAdd.validateInitialization();
		
		SqlSession sqlMap = db.getSqlSession(true);
		try
		{
			Identity ret = dbIdentities.insertIdentity(toAdd, null, sqlMap);
			
			if (!PersistentIdentity.ID.equals(toAdd.getTypeId()))
			{
				IdentityParam persistent = new IdentityParam(PersistentIdentity.ID, 
						PersistentIdentity.getNewId(),
						toAdd.isEnabled(), true);
				dbIdentities.insertIdentity(persistent, Long.parseLong(ret.getEntityId()), 
						sqlMap);
			}
			
			dbGroups.addMemberFromParent("/", new EntityParam(ret.getEntityId()), sqlMap);
			
			//TODO add the initial system attributes: lac, localAuthNState, attributeClasses and secrets.
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
			Entity ret = dbIdentities.getIdentitiesForEntity(entityId, sqlMap);
			//TODO set also credential info
			sqlMap.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}

	@Override
	public void setEntityCredentialRequirements(EntityParam entity, String requirementId,
			LocalCredentialState desiredAuthnState) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEntityCredential(EntityParam entity, String credentialId,
			String jsonConfiguration) throws EngineException
	{
		// TODO Auto-generated method stub
		
	}
}
