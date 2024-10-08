/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class IdentityEditorRegistry
{
	private final Map<String, IdentityEditorFactory> factoriesByType = new HashMap<>();

	@Autowired
	public IdentityEditorRegistry(List<IdentityEditorFactory> factories)
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

	public Set<String> getSupportedTypes()
	{
		return factoriesByType.keySet();
	}
}
