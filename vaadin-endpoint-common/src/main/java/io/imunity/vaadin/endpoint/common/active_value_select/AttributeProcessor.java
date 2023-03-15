/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.active_value_select;

import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import pl.edu.icm.unity.types.basic.DynamicAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class AttributeProcessor
{
	private final AttributeHandlerRegistry handlerRegistry;
	
	AttributeProcessor(AttributeHandlerRegistry handlerRegistry)
	{
		this.handlerRegistry = handlerRegistry;
	}
	
	private WebAttributeHandler getHandler(DynamicAttribute attr)
	{
		if (attr.getAttributeType() == null)
			return handlerRegistry.getHandlerWithStringFallback(attr.getAttribute());
		else
			return handlerRegistry.getHandlerWithStringFallback(attr.getAttributeType());
	}
	
	List<String> getValuesForPresentation(DynamicAttribute attribute)
	{
		WebAttributeHandler handler = getHandler(attribute);
		return attribute.getAttribute()	.getValues().stream()
				.map(handler::getValueAsString)
				.collect(Collectors.toList());
	}
	
	DynamicAttribute getAttributeWithActiveValues(DynamicAttribute full, List<Integer> activeValues)
	{
		DynamicAttribute ret = full.clone();
		List<String> values = new ArrayList<>();
		for (Integer index: activeValues)
			values.add(full.getAttribute().getValues().get(index));
		ret.getAttribute().setValues(values);
		return ret;
	}
}
