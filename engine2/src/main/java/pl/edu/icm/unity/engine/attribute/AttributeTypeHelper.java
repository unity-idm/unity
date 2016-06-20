/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Provides utilities allowing for easy access to common {@link AttributeType} related operations.
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeHelper
{
	private Map<String, AttributeValueSyntax<?>> unconfiguredSyntaxes;
	private AttributeSyntaxFactoriesRegistry atSyntaxRegistry;
	
	@Autowired
	public AttributeTypeHelper(AttributeSyntaxFactoriesRegistry atSyntaxRegistry)
	{
		this.atSyntaxRegistry = atSyntaxRegistry;
		unconfiguredSyntaxes = new HashMap<>();
		for (AttributeValueSyntaxFactory<?> f: atSyntaxRegistry.getAll())
			unconfiguredSyntaxes.put(f.getId(), f.createInstance());
	}
	
	public AttributeValueSyntax<?> getUnconfiguredSyntax(String name)
	{
		AttributeValueSyntax<?> ret = unconfiguredSyntaxes.get(name);
		if (ret == null)
			throw new IllegalArgumentException("There is no attribute defined with name " + name);
		return ret;
	}
	
	/**
	 * @param at
	 * @return configured value syntax for the attribute type
	 */
	public AttributeValueSyntax<?> getSyntax(AttributeType at)
	{
		AttributeValueSyntaxFactory<?> factory = atSyntaxRegistry.getByName(at.getValueSyntax());
		AttributeValueSyntax<?> ret = factory.createInstance();
		ret.setSerializedConfiguration(at.getValueSyntaxConfiguration());
		return ret;
	}
}
