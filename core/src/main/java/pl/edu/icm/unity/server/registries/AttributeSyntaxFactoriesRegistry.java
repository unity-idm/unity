/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.attributes.AttributeValueSyntaxFactory;

/**
 * Maintains a simple registry of available {@link AttributeValueSyntaxFactory}ies.
 * @author K. Benedyczak
 */
@Component
public class AttributeSyntaxFactoriesRegistry extends TypesRegistryBase<AttributeValueSyntaxFactory<?>>
{
	@Autowired
	public AttributeSyntaxFactoriesRegistry(Optional<List<AttributeValueSyntaxFactory<?>>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
	}

	@Override
	protected String getId(AttributeValueSyntaxFactory<?> from)
	{
		return from.getId();
	}
}
