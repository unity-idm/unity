/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.integration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a {@link IntegrationEventDefinition}s.
 * 
 * @author P. Piernik
 */
@Component
public class IntegrationEventRegistry extends TypesRegistryBase<IntegrationEventDefinition>
{

	@Autowired
	public IntegrationEventRegistry(List<IntegrationEventDefinition> typeElements)
	{
		super(typeElements);
	}
	
	@Override
	protected String getId(IntegrationEventDefinition from)
	{
		return from.getName();
	}

}