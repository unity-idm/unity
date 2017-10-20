/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import com.vaadin.v7.data.util.converter.StringToDoubleConverter;
import com.vaadin.v7.data.validator.DoubleRangeValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;

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
		removeAllValidators();
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		addValidator(new ConditionalRequiredValidator<Double>(msg, unlimited, Double.class));
		addValidator(new DoubleRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max));		
	}

	@Override
	public Class<? extends Double> getType()
	{
		return Double.class;
	}
}
