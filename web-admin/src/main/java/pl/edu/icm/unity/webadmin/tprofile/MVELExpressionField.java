/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import org.mvel2.MVEL;

import com.vaadin.v7.data.validator.AbstractStringValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

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
					String info;
					try
					{
						info = e.getMessage();
					} catch (Exception ee)
					{
						info = "Other MVEL error";
					}
					setErrorMessage(msg.getMessage("MVELExpressionField.invalidValueWithReason", 
							info));
					return false;
				}

				return true;
			}
		});
		setValidationVisible(false);
		setColumns(Styles.WIDE_TEXT_FIELD);
	}
}
