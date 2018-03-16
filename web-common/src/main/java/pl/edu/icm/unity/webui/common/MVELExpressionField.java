/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.mvel2.MVEL;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.Setter;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Field allowing for editing an MVEL expression
 * 
 * @author K. Benedyczak
 */
public class MVELExpressionField extends TextField
{
	private UnityMessageSource msg;

	public MVELExpressionField(UnityMessageSource msg, String caption, String description)
	{
		this.msg = msg;
		setCaption(caption);
		setDescription(description);
	}

	public void configureBinding(Binder<?> binder, String fieldName, boolean mandatory)
	{
		if (mandatory)
		{
			binder.forField(this).withValidator(getValidator(msg, mandatory))
					.asRequired(msg.getMessage("fieldRequired")).bind(fieldName);
					
		} else
		{
			binder.forField(this).withValidator(getValidator(msg, mandatory)).bind(fieldName);
		}

	}

	public <T> void configureBinding(Binder<String> binder,
			ValueProvider<String, String> getter, Setter<String, String> setter,
			boolean mandatory)
	{
		if (mandatory)
		{
			binder.forField(this).withValidator(getValidator(msg, mandatory))
					.asRequired(msg.getMessage("fieldRequired"))
					.bind(getter, setter);
		} else
		{
			binder.forField(this).withValidator(getValidator(msg, mandatory));
		}

	}

	private static Validator<String> getValidator(UnityMessageSource msg, boolean mandatory)
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
