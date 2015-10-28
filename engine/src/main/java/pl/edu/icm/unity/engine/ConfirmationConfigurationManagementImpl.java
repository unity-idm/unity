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
import pl.edu.icm.unity.db.generic.confirmation.ConfirmationConfigurationDB;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;

/**
 * Implementation of {@link ConfirmationConfigurationManagement}
 * 
 * @author P. Piernik
 */
@Component
@Transactional
@InvocationEventProducer
public class ConfirmationConfigurationManagementImpl implements ConfirmationConfigurationManagement
{
	private AuthorizationManager authz;
	private ConfirmationConfigurationDB configurationDB;
	
	@Autowired
	public ConfirmationConfigurationManagementImpl(AuthorizationManager authz, 
			ConfirmationConfigurationDB configurationDB)
	{

		this.authz = authz;
		this.configurationDB = configurationDB;
	}

	@Override
	public void addConfiguration(ConfirmationConfiguration toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		configurationDB.insert(toAdd.getTypeToConfirm() + toAdd.getNameToConfirm(),
					toAdd, sql);
	}

	@Override
	public void removeConfiguration(String typeToConfirm, String nameToConfirm)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		configurationDB.remove(typeToConfirm + nameToConfirm, sql);
	}

	@Override
	public void updateConfiguration(ConfirmationConfiguration toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		configurationDB.update(toUpdate.getTypeToConfirm() + toUpdate.getNameToConfirm(),
				toUpdate, sql);
	}

	@Override
	public ConfirmationConfiguration getConfiguration(String typeToConfirm, String nameToConfirm)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		return configurationDB.get(typeToConfirm + nameToConfirm, sql);
	}

	@Override
	public List<ConfirmationConfiguration> getAllConfigurations() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		SqlSession sql = SqlSessionTL.get();
		return configurationDB.getAll(sql);
	}

}
