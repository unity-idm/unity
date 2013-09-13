/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.GenericObjectHandler;

/**
 * Maintains a simple registry of available {@link GenericObjectHandler}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class GenericObjectHandlersRegistry extends TypesRegistryBase<GenericObjectHandler>
{
	@Autowired
	public GenericObjectHandlersRegistry(List<GenericObjectHandler> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(GenericObjectHandler from)
	{
		return from.getSupportedType();
	}
}
