/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.automation;

import org.quartz.CronExpression;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Field allowing for editing a Quartz cron expression
 * @author K. Benedyczak
 */
class CronExpressionField extends TextField
{
	private MessageSource msg;

	CronExpressionField(MessageSource msg, String caption)
	{
		this.msg = msg;
		setCaption(caption);
		setDescription(msg.getMessage("CronExpressionField.cronExpressionDescription"), ContentMode.HTML);
	}

	void configureBinding(Binder<?> binder, String fieldName)
	{
		binder.forField(this).withValidator(getValidator(msg))
				.asRequired(msg.getMessage("fieldRequired")).bind(fieldName);

	}

	private static Validator<String> getValidator(MessageSource msg)
	{
		Validator<String> expressionValidator = new Validator<String>()
		{

			@Override
			public ValidationResult apply(String value, ValueContext context)
			{
				try
				{
					CronExpression.validateExpression(value);
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
