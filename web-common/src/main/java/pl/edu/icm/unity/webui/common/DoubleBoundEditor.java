/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.util.converter.StringToDoubleConverter;
import com.vaadin.data.validator.DoubleRangeValidator;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public class DoubleBoundEditor extends AbstractBoundEditor<Double>
{
	public DoubleBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit,
			Double bound)
	{
		super(msg, labelUnlimited, labelLimit, bound, new StringToDoubleConverter());
	}

	@Override
	protected void updateValidators()
	{
		limit.removeAllValidators();
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		limit.addValidator(new DoubleRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max));		
	}
}
