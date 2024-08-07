/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
public class CredentialEditorRegistry
{
	private final Map<String, CredentialEditorFactory> factoriesByType = new HashMap<>();

	@Autowired
	public CredentialEditorRegistry(List<CredentialEditorFactory> factories)
	{
		for (CredentialEditorFactory factory: factories)
			factoriesByType.put(factory.getSupportedCredentialType(), factory);
	}

	public CredentialEditorFactory getFactory(String type)
	{
		CredentialEditorFactory factory = factoriesByType.get(type);
		if (factory == null)
			throw new IllegalArgumentException("Credential type " + type + 
					" has no editor factory registered");
		return factory;
	}
	
	public CredentialEditor getEditor(String type)
	{
		return getFactory(type).createCredentialEditor();
	}
	
	public Set<String> getSupportedTypes()
	{
		return factoriesByType.keySet();
	}
}
