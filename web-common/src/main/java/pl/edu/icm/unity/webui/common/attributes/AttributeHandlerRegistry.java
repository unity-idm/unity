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

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

/**
 * Gives access to web attribute handlers for given syntax types.
 * Additionally a methods are provided to easily get a simplified attribute representation for the given 
 * attribute.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeHandlerRegistry
{
	private Map<String, WebAttributeHandlerFactory> factoriesByType = new HashMap<String, WebAttributeHandlerFactory>();
	
	@Autowired
	public AttributeHandlerRegistry(List<WebAttributeHandlerFactory> factories)
	{
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
	
	/**
	 * Returns a string representing the attribute. The returned format contains the attribute name
	 * and the first value (if present). If more values are present this is only marked. The first value 
	 * is shortened if is long.
	 * @param attribute
	 * @return
	 */
	public String getSimplifiedAttributeRepresentation(Attribute<?> attribute)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(attribute.getName());
		List<?> values = attribute.getValues();
		if (values.size() > 0)
		{
			sb.append("=");
			sb.append(getSimplifiedAttributeValuesRepresentation(attribute));
		}
		return sb.toString();
	}
	
	/**
	 * Returns a string representing the attributes values. Only the first value is output (if present). 
	 * If more values are present this is only marked.
	 * @param attribute
	 * @return
	 */
	public String getSimplifiedAttributeValuesRepresentation(Attribute<?> attribute)
	{
		StringBuilder sb = new StringBuilder();
		List<?> values = attribute.getValues();
		if (values.size() > 0)
		{
			AttributeValueSyntax<?> syntax = attribute.getAttributeSyntax();
			@SuppressWarnings("rawtypes")
			WebAttributeHandler handler = getHandler(syntax.getValueSyntaxId());
			@SuppressWarnings("unchecked")
			String firstVal = handler.getValueAsString(values.get(0), syntax, 16);
			sb.append(firstVal);
			if (values.size() > 1)
				sb.append(", ...");
		}
		return sb.toString();
	}
}



