/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Action component allowing to select a number of days
 * @author K. Benedyczak
 */
public class DaysActionParameterComponent extends DefaultActionParameterComponent
{
	public DaysActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(desc, msg);
	}

	@Override
	protected void configureBinding(UnityMessageSource msg, boolean required)
	{
		binder = new Binder<>(String.class);
		binder.forField(this).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(msg.getMessage(
						"DaysActionParameterComponent.notANumber")))
				.withValidator(new IntegerRangeValidator(msg.getMessage(
						"DaysActionParameterComponent.notANumber"), 1,
						365 * 20))
				.bind(v -> Integer.valueOf(v), (c, v) -> {
					value = String.valueOf(v);
				});
		this.value = "0";
		binder.setBean(value);
	}
}
