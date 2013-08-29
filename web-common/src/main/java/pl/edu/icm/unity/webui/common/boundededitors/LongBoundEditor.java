/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
import pl.edu.icm.unity.webui.common.LongRangeValidator;
import pl.edu.icm.unity.webui.common.StringToLongConverter;

/**
 * Shows a checkbox and a textfield to query for a limit number with optional unlimited setting.
 * @author K. Benedyczak
 */
public class LongBoundEditor extends AbstractBoundEditor<Long>
{
	public LongBoundEditor(UnityMessageSource msg, String labelUnlimited, String labelLimit,
			Long bound)
	{
		super(msg, labelUnlimited, labelLimit, bound, new StringToLongConverter());
	}
	
	@Override
	protected void updateValidators()
	{
		removeAllValidators();
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		addValidator(new ConditionalRequiredValidator<Long>(msg, unlimited, Long.class));
		addValidator(new LongRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max));		
	}

	@Override
	public Class<? extends Long> getType()
	{
		return Long.class;
	}
}
