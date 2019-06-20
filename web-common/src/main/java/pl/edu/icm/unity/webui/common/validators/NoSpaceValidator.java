/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.validators;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

/**
 * Checks if value does not have spaces
 * 
 * @author P.Piernik
 */
public class NoSpaceValidator implements Validator<String>
{

	@Override
	public ValidationResult apply(String value, ValueContext context)
	{
		if (value != null && value.contains(" "))
		{
			return ValidationResult.error("NoSpaceValidator.noSpace");
		}

		return ValidationResult.ok();
	}

}
