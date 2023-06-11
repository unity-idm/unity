/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Custom field allowing for editing an {@link I18nString}, i.e. a string in several languages.
 * By default the default locale is only shown, but a special button can be clicked to show text fields
 * to enter translations.
 * 
 * @author K. Benedyczak
 */
public class I18nTextField extends Abstract18nField<TextField>
{
	public I18nTextField(MessageSource msg)
	{
		super(msg);
		initUI();
	}

	public I18nTextField(MessageSource msg, String caption)
	{
		super(msg, caption);
		initUI();
	}
	
	@Override
	protected TextField makeFieldInstance()
	{
		return new TextField();
	}
}
