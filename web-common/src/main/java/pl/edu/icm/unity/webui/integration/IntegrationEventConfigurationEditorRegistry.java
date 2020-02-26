/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.integration;

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
public class IntegrationEventConfigurationEditorRegistry extends TypesRegistryBase<IntegrationEventConfigurationEditorFactory>
{

	@Autowired
	public IntegrationEventConfigurationEditorRegistry(List<IntegrationEventConfigurationEditorFactory> typeElements)
	{
		super(typeElements);
	}
	
	@Override
	protected String getId(IntegrationEventConfigurationEditorFactory from)
	{
		return from.supportedType();
	}

}