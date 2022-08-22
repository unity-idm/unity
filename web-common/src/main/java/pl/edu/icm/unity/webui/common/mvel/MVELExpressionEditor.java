/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.mvel;

import org.mvel2.MVEL;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.Setter;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.AbstractField;

import pl.edu.icm.unity.MessageSource;

/**
 * Configures fields that can be used to edit MVAL expression.
 * 
 * @author K. Benedyczak
 */
class MVELExpressionEditor
{
	private MessageSource msg;
	private AbstractField<String> field;

	public MVELExpressionEditor(AbstractField<String> field, MessageSource msg, String caption, String description)
	{
		this.field = field;
		this.msg = msg;
		field.setCaption(caption);
		field.setDescription(description, ContentMode.HTML);
	}
	
	public MVELExpressionEditor(AbstractField<String> field, MessageSource msg)
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

	public static Validator<String> getValidator(MessageSource msg, boolean mandatory)
	{
		Validator<String> expressionValidator = new Validator<String>()
		{

			@Override
			public ValidationResult apply(String value, ValueContext context)
			{
				if (!mandatory && value == null)
					return ValidationResult.ok();
				try
				{
					MVEL.compileExpression(value);
					
				} catch (Exception e)
				{
					String info;
					try
					{
						info = e.getMessage();
					} catch (Exception ee)
					{
						info = "Other MVEL error";
					}

					return ValidationResult.error(msg.getMessage(
							"MVELExpressionField.invalidValueWithReason",
							info));

				}
				return ValidationResult.ok();
			}
		};
		return expressionValidator;
	}
}
