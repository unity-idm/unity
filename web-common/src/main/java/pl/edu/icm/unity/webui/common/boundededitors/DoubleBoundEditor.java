/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import com.vaadin.data.Binder;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.DoubleRangeValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public class DoubleBoundEditor extends AbstractBoundEditor<Double>
{

	public DoubleBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit,
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
	
	private static Validator<Double> getValidator(UnityMessageSource msg, Double min, Double max)
	{
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		return new DoubleRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max);		
	}
}
