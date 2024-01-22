/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.mvel;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.ValueProvider;
import org.mvel2.MVEL;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Configures fields that can be used to edit MVAL expression.
 */
class MVELExpressionEditor
{
	private final MessageSource msg;
	private final CustomField<String> field;
	
	MVELExpressionEditor(CustomField<String> field, MessageSource msg)
	{
		this.field = field;
		this.msg = msg;
	}

	void configureBinding(Binder<?> binder, String fieldName, boolean mandatory)
	{
		if (mandatory)
		{
			binder.forField(field).withValidator(getValidator(msg, mandatory))
					.asRequired(msg.getMessage("fieldRequired")).bind(fieldName);
					
		} else
		{
			binder.forField(field).withValidator(getValidator(msg, mandatory)).bind(fieldName);
		}

	}

	void configureBinding(Binder<String> binder,
						  ValueProvider<String, String> getter, Setter<String, String> setter,
						  boolean mandatory)
	{
		if (mandatory)
		{
			binder.forField(field).withValidator(getValidator(msg, mandatory))
					.asRequired(msg.getMessage("fieldRequired"))
					.bind(getter, setter);
		} else
		{
			binder.forField(field).withValidator(getValidator(msg, mandatory));
		}

	}

	static Validator<String> getValidator(MessageSource msg, boolean mandatory)
	{
		return (value, context) ->
		{
			if (!mandatory && value == null)
				return ValidationResult.ok();
			else if (mandatory && value == null)
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			try
			{
				MVEL.compileExpression(value);
			}
			catch (Exception e)
			{
				String info = e.getMessage();
				return ValidationResult.error(msg.getMessage(
						"MVELExpressionField.invalidValueWithReason",
						info));

			}
			return ValidationResult.ok();
		};
	}
}
