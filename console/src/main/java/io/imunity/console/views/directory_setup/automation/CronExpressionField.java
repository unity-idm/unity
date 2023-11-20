/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.automation;


import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import io.imunity.vaadin.elements.FormItemRequiredIndicatorHandler;
import org.quartz.CronExpression;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Field allowing for editing a Quartz cron expression
 */
class CronExpressionField extends TextField
{
	private final MessageSource msg;

	CronExpressionField(MessageSource msg, String caption)
	{
		this.msg = msg;
		setLabel(caption);
	}

	void configureBinding(Binder<?> binder, String fieldName)
	{
		binder.forField(this).withValidator(getValidator(msg))
				.asRequired(msg.getMessage("fieldRequired")).bind(fieldName);

	}

	private static Validator<String> getValidator(MessageSource msg)
	{
		return (value, context) ->
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
				return ValidationResult.error(msg.getMessage("CronExpressionField.invalidValueWithReason", info));

			}
			return ValidationResult.ok();
		};
	}

	@Override
	public void setInvalid(boolean invalid)
	{
		super.setInvalid(invalid);
		FormItemRequiredIndicatorHandler.setInvalid(this, invalid);
	}
}
