/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.boundededitors;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.CheckBox;

public class ConditionalRequiredValidator<T> extends AbstractValidator<T>
{
	private CheckBox condition;
	private Class<T> clazz;
	
	public ConditionalRequiredValidator(UnityMessageSource msg, CheckBox condition, Class<T> clazz)
	{
		super(msg.getMessage("fieldRequired"));
		this.condition = condition;
		this.clazz = clazz;
	}

	@Override
	protected boolean isValidValue(T value)
	{
		if (condition.getValue())
			return true;
		if (value == null || value.toString().equals(""))
			return false;
		return true;
	}

	@Override
	public Class<T> getType()
	{
		return clazz;
	}

}
