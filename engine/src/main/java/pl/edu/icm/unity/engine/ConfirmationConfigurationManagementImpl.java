/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.confirmation.ConfirmationConfigurationDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;

/**
 * Implementation of {@link ConfirmationConfigurationManagement}
 * 
 * @author P. Piernik
 */
@Component
@InvocationEventProducer
public class ConfirmationConfigurationManagementImpl implements ConfirmationConfigurationManagement
{
	private DBSessionManager db;
	private AuthorizationManager authz;
	private ConfirmationConfigurationDB configurationDB;
	
	@Autowired
	public ConfirmationConfigurationManagementImpl(DBSessionManager db,
			AuthorizationManager authz, ConfirmationConfigurationDB configurationDB)
	{

		this.db = db;
		this.authz = authz;
		this.configurationDB = configurationDB;
	}

	@Override
	public void addConfiguration(ConfirmationConfiguration toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			configurationDB.insert(toAdd.getTypeToConfirm() + toAdd.getNameToConfirm(),
					toAdd, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

	}

	@Override
	public void removeConfiguration(String typeToConfirm, String nameToConfirm)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			configurationDB.remove(typeToConfirm + nameToConfirm, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

	}

	@Override
	public void updateConfiguration(ConfirmationConfiguration toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			configurationDB.update(
					toUpdate.getTypeToConfirm() + toUpdate.getNameToConfirm(),
					toUpdate, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public ConfirmationConfiguration getConfiguration(String typeToConfirm, String nameToConfirm)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		ConfirmationConfiguration configuration = null;
		try
		{
			configuration = configurationDB.get(typeToConfirm + nameToConfirm, sql);
			sql.commit();
			return configuration;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public List<ConfirmationConfiguration> getAllConfigurations() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<ConfirmationConfiguration> ret;
			ret = configurationDB.getAll(sql);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

}
