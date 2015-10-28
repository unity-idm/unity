/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.realm.RealmDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.RealmsManagement;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Implementation of {@link RealmsManagement}.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class RealmsManagementImpl implements RealmsManagement
{
	private DBSessionManager db;
	private RealmDB realmDB;
	private AuthorizationManager authz;
	
	@Autowired
	public RealmsManagementImpl(DBSessionManager db, RealmDB realmDB, AuthorizationManager authz)
	{
		super();
		this.db = db;
		this.realmDB = realmDB;
		this.authz = authz;
	}

	@Override
	public void addRealm(AuthenticationRealm realm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			realmDB.insert(realm.getName(), realm, sql);
			sql.commit();
		} catch (Exception e)
		{
			throw new EngineException("Unable to create a realm: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public AuthenticationRealm getRealm(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			AuthenticationRealm ret = realmDB.get(name, sql);
			sql.commit();
			return ret;
		} catch (Exception e)
		{
			throw new EngineException("Unable to retrieve a realm: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public Collection<AuthenticationRealm> getRealms() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Collection<AuthenticationRealm> ret = realmDB.getAll(sql);
			sql.commit();
			return ret;
		} catch (Exception e)
		{
			throw new EngineException("Unable to retrieve realms: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void updateRealm(AuthenticationRealm realm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			realmDB.update(realm.getName(), realm, sql);
			sql.commit();
		} catch (Exception e)
		{
			throw new EngineException("Unable to update a realm: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public void removeRealm(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			realmDB.remove(name, sql);
			sql.commit();
		} catch (Exception e)
		{
			throw new EngineException("Unable to remove a realm: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
}
