/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * Implementation of identity types management. Responsible for top level transaction handling,
 * proper error logging and authorization.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
@Primary
public class IdentityTypeManagementImpl implements IdentityTypesManagement
{
	private IdentityTypeDAO dbIdentities;
	private InternalAuthorizationManager authz;
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired
	public IdentityTypeManagementImpl(IdentityTypeDAO dbIdentities, 
			InternalAuthorizationManager authz, IdentityTypesRegistry idTypesRegistry)
	{
		this.dbIdentities = dbIdentities;
		this.authz = authz;
		this.idTypesRegistry = idTypesRegistry;
	}

	@Override
	@Transactional
	public Collection<IdentityType> getIdentityTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return dbIdentities.getAll();
	}
	
	@Override
	@Transactional
	public IdentityType getIdentityType(String name) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return dbIdentities.get(name);
	}

	@Transactional
	@Override
	public void updateIdentityType(IdentityType toUpdate) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toUpdate.getIdentityTypeProvider());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		if (toUpdate.getMinInstances() < 0)
			throw new IllegalAttributeTypeException("Minimum number of instances "
					+ "can not be negative");
		if (toUpdate.getMinVerifiedInstances() > toUpdate.getMinInstances())
			throw new IllegalAttributeTypeException("Minimum number of verified instances "
					+ "can not be larger then the regular minimum of instances");
		if (toUpdate.getMinInstances() > toUpdate.getMaxInstances())
			throw new IllegalAttributeTypeException("Minimum number of instances "
					+ "can not be larger then the maximum");
		dbIdentities.update(toUpdate);
	}
}
