/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives access to {@link WebAttributeMetadataHandler}s for given metadata types.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeMetadataHandlerRegistry
{
	private final Map<String, WebAttributeMetadataHandlerFactory> factoriesByType =
			new HashMap<String, WebAttributeMetadataHandlerFactory>();
	
	@Autowired
	public AttributeMetadataHandlerRegistry(List<WebAttributeMetadataHandlerFactory> factories)
	{
		for (WebAttributeMetadataHandlerFactory factory: factories)
			factoriesByType.put(factory.getSupportedMetadata(), factory);
	}
	
	public WebAttributeMetadataHandler getHandler(String metadataId)
	{
		WebAttributeMetadataHandlerFactory factory = factoriesByType.get(metadataId);
		if (factory == null)
			throw new IllegalArgumentException("Metadata " + metadataId + " has no handler factory registered");
		return factory.newInstance();
	}
	
	public Set<String> getSupportedSyntaxes()
	{
		return new HashSet<>(factoriesByType.keySet());
	}
}



