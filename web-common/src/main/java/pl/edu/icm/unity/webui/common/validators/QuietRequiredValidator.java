/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.validators;

import com.google.common.base.Predicate;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

import pl.edu.icm.unity.base.message.MessageSource;

public class QuietRequiredValidator<T> implements Validator<T>
{
	private MessageSource msg;
	private Predicate<T> empty;
	
	
	public QuietRequiredValidator(MessageSource msg)
	{
		this.msg = msg;
		this.empty = v -> false;
		
	}
	
	public QuietRequiredValidator(MessageSource msg, Predicate<T> empty)
	{
		this.msg = msg;
		this.empty = empty;
	}
	
	@Override
	public ValidationResult apply(T value, ValueContext context)
	{
		if (value == null || empty.apply(value))
		{
			return ValidationResult.error(msg.getMessage("fieldRequired"));
		}

		return ValidationResult.ok();
	}

}
