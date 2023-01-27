/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.plugins.attributes.bounded_editors;


import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

public class DoubleBoundEditor extends AbstractBoundEditor<Double>
{

	public DoubleBoundEditor(MessageSource msg, String labelUnlimited, String labelLimit,
			Double bound, Double min, Double max)
	{
		super(msg, labelUnlimited, labelLimit, bound, min, max);
	}

	
	public void configureBinding(Binder<?> binder, String fieldName)
	{
		binder.forField(this)
			.withConverter(vW -> vW.isUnlimited() ? bound : Double.parseDouble(vW.getValue()), 
					v -> new ValueWrapper(String.valueOf(v), v.equals(bound)),
					msg.getMessage("DoubleBoundEditor.notANumber"))
			.withValidator(getValidator(msg, min, max))
			.bind(fieldName);
	}
	
	private static Validator<Double> getValidator(MessageSource msg, Double min, Double max)
	{
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		return new DoubleRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range),
				min, max);		
	}
}
