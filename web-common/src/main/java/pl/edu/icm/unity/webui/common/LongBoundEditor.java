/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

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
		limit.removeAllValidators();
		String range = AttributeTypeUtils.getBoundsDesc(msg, min, max);
		limit.addValidator(new LongRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError", range), 
				min, max));		
	}
}
