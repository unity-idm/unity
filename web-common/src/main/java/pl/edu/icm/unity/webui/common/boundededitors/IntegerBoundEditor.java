/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import com.vaadin.data.Binder;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.IntegerRangeValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public class IntegerBoundEditor extends AbstractBoundEditor<Integer>
{

	public IntegerBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit,
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
	
	private static Validator<Integer> getValidator(UnityMessageSource msg, Integer min, Integer max)
	{
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		return new IntegerRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max);		
	}
}
