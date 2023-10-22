/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors;

import java.util.Optional;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;


public class IntegerBoundEditor extends AbstractBoundEditor<Integer>
{

	public IntegerBoundEditor(MessageSource msg, String labelUnlimited, Optional<String> labelLimit,
			Integer bound, Integer min, Integer max)
	{
		super(msg, labelUnlimited, labelLimit, bound, min, max);
	}

	
	public void configureBinding(Binder<?> binder, String fieldName)
	{
		binder.forField(this)
			.withConverter(vW -> vW.isUnlimited() ? bound : Integer.parseInt(vW.getValue()), 
					v -> new ValueWrapper(String.valueOf(v), v.equals(bound)),
					msg.getMessage("IntegerBoundEditor.notANumber"))
			.withValidator(getValidator(msg, min, max))
			.bind(fieldName);
	}
	
	private static Validator<Integer> getValidator(MessageSource msg, Integer min, Integer max)
	{
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		return new IntegerRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range),
				min, max);		
	}
}
