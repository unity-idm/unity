/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import org.mvel2.MVEL;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.data.validator.AbstractStringValidator;

/**
 * Field allowing for editing an MVEL expression
 * @author K. Benedyczak
 */
public class MVELExpressionField extends RequiredTextField
{
	private UnityMessageSource msg;
	
	public MVELExpressionField(UnityMessageSource msg, String caption, String description)
	{
		super(caption, msg);
		this.msg = msg;
		setDescription(description);
		initUI();
	}

	private void initUI()
	{
		addValidator(new AbstractStringValidator(
				msg.getMessage("MVELExpressionField.invalidValue"))
		{
			@Override
			protected boolean isValidValue(String value)
			{
				try
				{
					MVEL.compileExpression(value);
				} catch (Exception e)
				{
					setErrorMessage(msg.getMessage("MVELExpressionField.invalidValueWithReason", 
							e.getMessage()));
					return false;
				}

				return true;
			}
		});
		setValidationVisible(false);
		setColumns(Styles.WIDE_TEXT_FIELD);
	}
}
