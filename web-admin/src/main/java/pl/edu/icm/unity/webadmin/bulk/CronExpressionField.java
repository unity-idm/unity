/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import org.quartz.CronExpression;

import com.vaadin.v7.data.validator.AbstractStringValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.RequiredTextField;

/**
 * Field allowing for editing a Quartz cron expression
 * @author K. Benedyczak
 */
public class CronExpressionField extends RequiredTextField
{
	private UnityMessageSource msg;
	
	public CronExpressionField(UnityMessageSource msg, String caption)
	{
		super(caption, msg);
		this.msg = msg;
		setDescription(msg.getMessage("CronExpressionField.cronExpressionDescription"));
		initUI();
	}

	private void initUI()
	{
		addValidator(new AbstractStringValidator(
				msg.getMessage("CronExpressionField.invalidValue"))
		{
			@Override
			protected boolean isValidValue(String value)
			{
				try
				{
					CronExpression.validateExpression(value);
				} catch (Exception e)
				{
					setErrorMessage(msg.getMessage("CronExpressionField.invalidValueWithReason", 
							e.getMessage()));
					return false;
				}

				return true;
			}
		});
	}
}
