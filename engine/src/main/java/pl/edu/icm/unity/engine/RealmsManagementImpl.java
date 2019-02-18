/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
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
	private InternalAuthorizationManager authz;
	
	@Autowired
	public RealmsManagementImpl(RealmDB realmDB, InternalAuthorizationManager authz)
	{
		super();
		this.realmDB = realmDB;
		this.authz = authz;
	}

	@Override
	public void addRealm(AuthenticationRealm realm) throws EngineException
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
	public AuthenticationRealm getRealm(String name) throws EngineException
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
	public Collection<AuthenticationRealm> getRealms() throws EngineException
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

	@Override
	public void updateRealm(AuthenticationRealm realm) throws EngineException
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
	public void removeRealm(String name) throws EngineException
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
}
