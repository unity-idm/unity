/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public class IntegerBoundEditor extends AbstractBoundEditor<Integer>
{
	public IntegerBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit,
			Integer bound)
	{
		super(msg, labelUnlimited, labelLimit, bound, new StringToIntegerConverter());
	}

	@Override
	protected void updateValidators()
	{
		limit.removeAllValidators();
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		limit.addValidator(new IntegerRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max));		
	}

	@Override
	public Class<? extends Integer> getType()
	{
		return Integer.class;
	}
}
