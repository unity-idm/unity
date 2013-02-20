/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.types.AuthenticationSecret;
import pl.edu.icm.unity.types.Entity;
import pl.edu.icm.unity.types.EntityLAC;
import pl.edu.icm.unity.types.EntityParam;
import pl.edu.icm.unity.types.Identity;
import pl.edu.icm.unity.types.IdentityParam;
import pl.edu.icm.unity.types.IdentityTaV;
import pl.edu.icm.unity.types.IdentityType;
import pl.edu.icm.unity.types.LocalAuthnState;

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

	@Autowired
	public IdentitiesManagementImpl(DBSessionManager db, DBIdentities dbIdentities)
	{
		this.db = db;
		this.dbIdentities = dbIdentities;
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
	public Identity addIdentity(IdentityTaV toAdd, String lacId) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Identity addIdentity(IdentityTaV toAdd, EntityParam parentEntity)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeIdentity(IdentityParam toRemove) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setIdentityStatus(IdentityParam toChange, boolean status)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Entity getEntity(EntityParam entity) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEntityLAC(EntityParam entity, String lacId, LocalAuthnState desiredAuthnState)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityLAC getEntityLAC(EntityParam entity) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setEntityAuthenticationSecrets(EntityParam entity,
			List<AuthenticationSecret> secrets) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}
}
