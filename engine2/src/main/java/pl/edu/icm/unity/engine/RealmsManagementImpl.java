/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Implementation of {@link RealmsManagement}.
 * @author K. Benedyczak
 */
@Component
@Primary
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
		create(realm);
	}

	@Override
	public AuthenticationRealm getRealm(String name) throws EngineException
	{
		return get(name);
	}

	@Override
	public Collection<AuthenticationRealm> getRealms() throws EngineException
	{
		return getAll();
	}

	@Override
	public void updateRealm(AuthenticationRealm realm) throws EngineException
	{
		update(realm);
	}

	@Override
	public void removeRealm(String name) throws EngineException
	{
		deleteByName(name);
	}

	@Override
	public void updateByName(String name, AuthenticationRealm newValue) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			realmDB.updateByName(name, newValue);
		} catch (Exception e)
		{
			throw new EngineException("Unable to remove a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public void deleteByName(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			realmDB.delete(name);
		} catch (Exception e)
		{
			throw new EngineException("Unable to remove a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean exists(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			return realmDB.exists(name);
		} catch (Exception e)
		{
			throw new EngineException("Unable to retrieve a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public AuthenticationRealm get(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			return realmDB.get(name);
		} catch (Exception e)
		{
			throw new EngineException("Unable to retrieve a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public void create(AuthenticationRealm realm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			realmDB.create(realm);
		} catch (Exception e)
		{
			throw new EngineException("Unable to create a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public void update(AuthenticationRealm realm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			realmDB.update(realm);
		} catch (Exception e)
		{
			throw new EngineException("Unable to update a realm: " + e.getMessage(), e);
		}
	}

	@Override
	public void delete(AuthenticationRealm realm) throws EngineException
	{
		deleteByName(realm.getName());
	}

	@Override
	public List<AuthenticationRealm> getAll() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		try
		{
			return realmDB.getAll();
		} catch (Exception e)
		{
			throw new EngineException("Unable to retrieve realms: " + e.getMessage(), e);
		}
	}
}
