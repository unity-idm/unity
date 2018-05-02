/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.UnknownIdentityException;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Resolves {@link EntityParam} into entityId.
 * @author K. Benedyczak
 */
@Component
public class EntityResolverImpl implements EntityResolver
{
	private EntityDAO entityDAO;
	private IdentityDAO identityDAO;
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired
	public EntityResolverImpl(EntityDAO entityDAO, IdentityDAO identityDAO,
			IdentityTypesRegistry idTypesRegistry)
	{
		this.entityDAO = entityDAO;
		this.identityDAO = identityDAO;
		this.idTypesRegistry = idTypesRegistry;
	}

	@Override
	public long getEntityId(IdentityTaV entity) throws IllegalIdentityValueException
	{
		return getFullIdentity(entity).getEntityId();
	}

	@Override
	public long getEntityId(EntityParam entity) throws IllegalIdentityValueException
	{
		if (entity.getIdentity() != null)
			return getEntityId(entity.getIdentity());
		try
		{
			entityDAO.getByKey(entity.getEntityId());
			return entity.getEntityId();
		} catch (IllegalArgumentException e)
		{
			throw new UnknownIdentityException("Entity " + entity + " is unknown", e);
		}
	}

	@Transactional
	@Override
	public Identity getFullIdentity(IdentityTaV entity) throws IllegalIdentityValueException
	{
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(entity.getTypeId());
		String comparableValue = idTypeDef.getComparableValue(entity.getValue(), entity.getRealm(), 
				entity.getTarget());
		try
		{
			return identityDAO.get(StoredIdentity.toInDBIdentityValue(entity.getTypeId(), comparableValue))
				.getIdentity();
		} catch (IllegalArgumentException e)
		{
			throw new UnknownIdentityException("Entity " + entity + " is unknown", e);
		}
	}
}
