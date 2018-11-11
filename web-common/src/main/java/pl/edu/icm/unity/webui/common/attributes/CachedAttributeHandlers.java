/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Provides fast attributes web handling with minimal overhead of accessing engine, as attribute handlers are 
 * cached on the way. Useful for use in loops performing many operations on attributes.
 *  
 * @author K. Benedyczak
 */
public class CachedAttributeHandlers
{
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private Map<String, WebAttributeHandler> handlersByAttrName = new HashMap<>();
	
	public CachedAttributeHandlers(AttributeHandlerRegistry attributeHandlerRegistry)
	{
		this.attributeHandlerRegistry = attributeHandlerRegistry;
	}


	public String getSimplifiedAttributeValuesRepresentation(Attribute attribute)
	{
		WebAttributeHandler webAttributeHandler = handlersByAttrName.get(attribute.getName());
		if (webAttributeHandler == null)
		{
			webAttributeHandler = attributeHandlerRegistry.getHandlerWithStringFallback(attribute);
			handlersByAttrName.put(attribute.getName(), webAttributeHandler);
		}
		return attributeHandlerRegistry.getSimplifiedAttributeValuesRepresentation(attribute, 
				webAttributeHandler);
	}
}
