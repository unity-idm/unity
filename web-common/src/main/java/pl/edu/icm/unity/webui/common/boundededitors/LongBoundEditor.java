/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import com.vaadin.data.Binder;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.LongRangeValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public class LongBoundEditor extends AbstractBoundEditor<Long>
{

	public LongBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit,
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
	
	private static Validator<Long> getValidator(UnityMessageSource msg, Long min, Long max)
	{
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		return new LongRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max);		
	}
}
