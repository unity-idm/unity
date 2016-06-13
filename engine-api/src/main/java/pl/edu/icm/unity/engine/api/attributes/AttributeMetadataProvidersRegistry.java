/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.registries.TypesRegistryBase;

@Component
public class AttributeMetadataProvidersRegistry extends TypesRegistryBase<AttributeMetadataProvider>
{
	@Autowired
	public AttributeMetadataProvidersRegistry(Optional<List<AttributeMetadataProvider>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
	}

	@Override
	protected String getId(AttributeMetadataProvider from)
	{
		return from.getName();
	}

}
