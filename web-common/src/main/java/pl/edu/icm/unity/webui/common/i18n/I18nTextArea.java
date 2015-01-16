/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;

import com.vaadin.ui.TextArea;

/**
 * Custom field allowing for editing an {@link I18nString}, i.e. a string in several languages.
 * By default the default locale is only shown, but a special button can be clicked to show text fields
 * to enter translations.
 * <p>
 * This class is using TextArea as an internal edit component, so is suitable for longer texts.
 * 
 * @author K. Benedyczak
 */
public class I18nTextArea extends Abstract18nField<TextArea>
{
	public I18nTextArea(UnityMessageSource msg)
	{
		super(msg);
	}

	public I18nTextArea(UnityMessageSource msg, String caption)
	{
		super(msg, caption);
	}
	
	@Override
	protected TextArea makeFieldInstance()
	{
		TextArea ret = new TextArea();
		ret.setRows(2);
		ret.setWidth(100, Unit.PERCENTAGE);
		ret.setWordwrap(true);
		return ret;
	}
}
