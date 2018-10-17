/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon.activesel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

/**
 * Converts {@link DynamicAttribute} to presentation form and return its version with selected values only attributes
 * @author K. Benedyczak
 */
class AttributeProcessor
{
	private AttributeHandlerRegistry handlerRegistry;
	
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
				.map(raw -> handler.getValueAsString(raw))
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
