/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.tprofile;

import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;

/**
 * Action component allowing to select a number of days
 * @author K. Benedyczak
 */
public class DaysActionParameterComponent extends DefaultActionParameterComponent
{
	public DaysActionParameterComponent(ActionParameterDefinition desc, MessageSource msg)
	{
		super(desc, msg);
	}

	@Override
	protected void configureBinding(MessageSource msg, boolean mandatory)
	{
		if (mandatory)
		{
			binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
					.withConverter(new StringToIntegerConverter(msg.getMessage(
							"DaysActionParameterComponent.notANumber")))
					.withValidator(new IntegerRangeValidator(msg.getMessage(
							"DaysActionParameterComponent.notANumber"),
							1, 365 * 20))
					.withConverter(v -> String.valueOf(v),
							v -> Integer.valueOf(v))
					.bind("value");
			binder.setBean(new StringValueBean("1"));
		} else
		{
			binder.forField(this)
					.withConverter(new StringToIntegerConverter(msg.getMessage(
							"DaysActionParameterComponent.notANumber")))
					.withValidator(new IntegerRangeValidator(msg.getMessage(
							"DaysActionParameterComponent.notANumber"),
							1, 365 * 20))
					.withConverter(v -> String.valueOf(v),
							v -> Integer.valueOf(v))
					.bind("value");
			binder.setBean(new StringValueBean());
		}
	}
}
