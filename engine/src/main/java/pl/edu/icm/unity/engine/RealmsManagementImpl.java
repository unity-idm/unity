/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.realm.RealmDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.RealmsManagement;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Implementation of {@link RealmsManagement}.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
@Transactional
public class RealmsManagementImpl implements RealmsManagement
{
	private RealmDB realmDB;
	private AuthorizationManager authz;
	
	@Autowired
	public RealmsManagementImpl(RealmDB realmDB, AuthorizationManager authz)
	{
		super();
		this.realmDB = realmDB;
		this.authz = authz;
	}

	@Override
	public void addRealm(AuthenticationRealm realm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		try
		{
			realmDB.insert(realm.getName(), realm, sql);
		} catch (Exception e)
		{
			throw new EngineException("Unable to create a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public AuthenticationRealm getRealm(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		try
		{
			return realmDB.get(name, sql);
		} catch (Exception e)
		{
			throw new EngineException("Unable to retrieve a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public Collection<AuthenticationRealm> getRealms() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		try
		{
			return realmDB.getAll(sql);
		} catch (Exception e)
		{
			throw new EngineException("Unable to retrieve realms: " + e.getMessage(), e);
		}
	}

	@Override
	public void updateRealm(AuthenticationRealm realm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		try
		{
			realmDB.update(realm.getName(), realm, sql);
		} catch (Exception e)
		{
			throw new EngineException("Unable to update a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public void removeRealm(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		try
		{
			realmDB.remove(name, sql);
		} catch (Exception e)
		{
			throw new EngineException("Unable to remove a realm: " + e.getMessage(), e);
		}
	}
}
