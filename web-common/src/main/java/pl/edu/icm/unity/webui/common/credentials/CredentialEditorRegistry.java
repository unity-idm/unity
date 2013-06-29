/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives access to web credential editors for given credential types.
 * @author K. Benedyczak
 */
@Component
public class CredentialEditorRegistry
{
	private Map<String, CredentialEditorFactory> factoriesByType = new HashMap<String, CredentialEditorFactory>();

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
