/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.generic;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.registries.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link GenericObjectHandler}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class GenericObjectHandlersRegistry extends TypesRegistryBase<GenericEntityHandler<?>>
{
	@Autowired
	public GenericObjectHandlersRegistry(List<GenericEntityHandler<?>> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(GenericEntityHandler<?> from)
	{
		return from.getType();
	}
}
