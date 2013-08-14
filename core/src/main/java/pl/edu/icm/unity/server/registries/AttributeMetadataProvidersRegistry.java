/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.attributes.AttributeMetadataProvider;

@Component
public class AttributeMetadataProvidersRegistry extends TypesRegistryBase<AttributeMetadataProvider>
{
	@Autowired
	public AttributeMetadataProvidersRegistry(List<AttributeMetadataProvider> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(AttributeMetadataProvider from)
	{
		return from.getName();
	}

}
