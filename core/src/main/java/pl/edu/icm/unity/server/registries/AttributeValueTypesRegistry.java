/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

/**
 * Maintains a simple registry of available {@link AttributeValueSyntax}es.
 * FIXME - change the name.
 * @author K. Benedyczak
 */
@Component
public class AttributeValueTypesRegistry extends TypesRegistryBase<AttributeValueSyntaxFactory<?>>
{
	@Autowired
	public AttributeValueTypesRegistry(List<AttributeValueSyntaxFactory<?>> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(AttributeValueSyntaxFactory<?> from)
	{
		return from.getId();
	}
}
