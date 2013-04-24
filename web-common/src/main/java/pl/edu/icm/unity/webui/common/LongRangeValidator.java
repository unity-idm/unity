/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.data.validator.RangeValidator;

public class LongRangeValidator extends RangeValidator<Long>
{
	public LongRangeValidator(String errorMessage, Long minValue, Long maxValue)
	{
		super(errorMessage, Long.class, minValue, maxValue);
	}
}
