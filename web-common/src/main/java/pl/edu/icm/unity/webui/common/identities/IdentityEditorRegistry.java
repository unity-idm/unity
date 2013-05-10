/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives access to web identity editors for given identity types.
 * @author K. Benedyczak
 */
@Component
public class IdentityEditorRegistry
{
	private Map<String, IdentityEditorFactory> factoriesByType = new HashMap<String, IdentityEditorFactory>();

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
