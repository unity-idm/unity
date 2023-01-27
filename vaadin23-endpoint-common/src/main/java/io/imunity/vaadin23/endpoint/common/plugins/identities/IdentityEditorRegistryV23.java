/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.plugins.identities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class IdentityEditorRegistryV23
{
	private final Map<String, IdentityEditorFactory> factoriesByType = new HashMap<>();

	@Autowired
	public IdentityEditorRegistryV23(List<IdentityEditorFactory> factories)
	{
		super();
		for (IdentityEditorFactory factory: factories)
			factoriesByType.put(factory.getSupportedIdentityType(), factory);
	}
	
	public IdentityEditor getEditor(String type)
	{
		IdentityEditorFactory factory = factoriesByType.get(type);
		if (factory == null)
			throw new IllegalArgumentException("Identity type " + type + " has no editor factory registered");
		return factory.createInstance();
	}
}
