/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;

public class I18nOptionalLangTextField extends AbstractOptionalLang18nField<TextField>
{
	public I18nOptionalLangTextField(MessageSource msg)
	{
		super(msg);
		initUI();
	}

	public I18nOptionalLangTextField(MessageSource msg, String caption)
	{
		super(msg, caption);
		initUI();
	}
	
	@Override
	protected TextField makeFieldInstance()
	{
		TextField field = new TextField();
		field.setWidth(100, Unit.PERCENTAGE);
		return field;
	}
}
