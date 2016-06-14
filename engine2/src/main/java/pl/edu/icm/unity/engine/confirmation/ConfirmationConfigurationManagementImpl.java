/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.ConfirmationConfigurationDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration;

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
		configurationDB.create(toAdd);
	}

	@Override
	public void removeConfiguration(String typeToConfirm, String nameToConfirm)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		configurationDB.delete(typeToConfirm + nameToConfirm);
	}

	@Override
	public void updateConfiguration(ConfirmationConfiguration toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		configurationDB.update(toUpdate);
	}

	@Override
	public ConfirmationConfiguration getConfiguration(String typeToConfirm, String nameToConfirm)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return configurationDB.get(typeToConfirm + nameToConfirm);
	}

	@Override
	public List<ConfirmationConfiguration> getAllConfigurations() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return configurationDB.getAll();
	}

}
