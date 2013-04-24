/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives access to web attribute handlers for given syntax types.
 * @author K. Benedyczak
 */
@Component
public class AttributeHandlerRegistry
{
	private Map<String, WebAttributeHandlerFactory> factoriesByType = new HashMap<String, WebAttributeHandlerFactory>();

	@Autowired
	public AttributeHandlerRegistry(List<WebAttributeHandlerFactory> factories)
	{
		super();
		for (WebAttributeHandlerFactory factory: factories)
			factoriesByType.put(factory.getSupportedSyntaxId(), factory);
	}
	
	public WebAttributeHandler<?> getHandler(String syntaxId)
	{
		WebAttributeHandlerFactory factory = factoriesByType.get(syntaxId);
		if (factory == null)
			throw new IllegalArgumentException("SyntaxId " + syntaxId + " has no handler factory registered");
		return factory.createInstance();
	}
	
	public Set<String> getSupportedSyntaxes()
	{
		return factoriesByType.keySet();
	}
}
