/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.types.AttributeValueSyntax;

/**
 * Maintains a simple registry of available {@link AttributeValueSyntax}es.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeValueTypesRegistry extends TypesRegistryBase<AttributeValueSyntax<?>>
{
	@Autowired
	public AttributeValueTypesRegistry(List<AttributeValueSyntax<?>> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(AttributeValueSyntax<?> from)
	{
		return from.getValueSyntaxId();
	}
}
