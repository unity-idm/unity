/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.types.IdentityTypeDefinition;

/**
 * Maintains a simple registry of available {@link IdentityTypeDefinition}s.
 * 
 * @author K. Benedyczak
 */
@Component
public class IdentityTypesRegistry extends TypesRegistryBase<IdentityTypeDefinition>
{
	@Autowired
	public IdentityTypesRegistry(List<IdentityTypeDefinition> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(IdentityTypeDefinition from)
	{
		return from.getId();
	}
}
