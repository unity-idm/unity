/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulkops;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;


/**
 * Maintains a simple registry of available {@link EntityActionFactory}ies.
 * 
 * @author K. Benedyczak
 */
@Component
public class EntityActionsRegistry extends TypesRegistryBase<EntityActionFactory>
{
	@Autowired
	public EntityActionsRegistry(List<EntityActionFactory> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(EntityActionFactory from)
	{
		return from.getActionType().getName();
	}
}
