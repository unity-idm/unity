/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors;

import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.LongRangeValidator;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

public class LongBoundEditor extends AbstractBoundEditor<Long>
{

	public LongBoundEditor(MessageSource msg, String labelUnlimited, String labelLimit,
			Long bound, Long min, Long max)
	{
		super(msg, labelUnlimited, labelLimit, bound, min, max);
	}

	
	public void configureBinding(Binder<?> binder, String fieldName)
	{
		binder.forField(this)
			.withConverter(vW -> vW.isUnlimited() ? bound : Long.parseLong(vW.getValue()), 
					v -> new ValueWrapper(String.valueOf(v), v.equals(bound)),
					msg.getMessage("LongBoundEditor.notANumber"))
			.withValidator(getValidator(msg, min, max))
			.bind(fieldName);
	}
	
	private static Validator<Long> getValidator(MessageSource msg, Long min, Long max)
	{
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		return new LongRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range),
				min, max);		
	}
}
