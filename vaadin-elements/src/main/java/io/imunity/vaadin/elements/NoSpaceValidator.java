/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;

import java.util.function.Function;

public class NoSpaceValidator implements Validator<String>
{

	private final Function<String, String> msg;

	public NoSpaceValidator(Function<String, String> msg)
	{
		this.msg = msg;
	}


	@Override
	public ValidationResult apply(String value, ValueContext context)
	{
		if (value != null && value.contains(" "))
		{
			return ValidationResult.error(msg.apply("NoSpaceValidator.noSpace"));
		}

		return ValidationResult.ok();
	}

}
