/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import com.vaadin.ui.TextArea;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;

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
	private int rows = 2;
	
	public I18nTextArea(UnityMessageSource msg)
	{
		super(msg);
		initUI();
	}

	public I18nTextArea(UnityMessageSource msg, String caption)
	{
		super(msg, caption);
		initUI();
	}
	
	public I18nTextArea(UnityMessageSource msg, String caption, int rows)
	{
		super(msg, caption);
		this.rows = rows;
		initUI();
	}
	
	@Override
	protected TextArea makeFieldInstance()
	{
		TextArea ret = new TextArea();
		ret.setRows(rows);
		ret.setWidth(100, Unit.PERCENTAGE);
		ret.setWordWrap(true);
		return ret;
	}
}
