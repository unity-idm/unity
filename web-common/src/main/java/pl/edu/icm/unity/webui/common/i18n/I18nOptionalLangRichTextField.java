/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import com.vaadin.ui.RichTextArea;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

public class I18nOptionalLangRichTextField extends AbstractOptionalLang18nField<RichTextArea>
{
	public I18nOptionalLangRichTextField(UnityMessageSource msg)
	{
		super(msg);
		initUI();
	}

	public I18nOptionalLangRichTextField(UnityMessageSource msg, String caption)
	{
		super(msg, caption);
		initUI();
	}

	@Override
	protected RichTextArea makeFieldInstance()
	{
		RichTextArea ret = new RichTextArea();
		ret.setWidth(100, Unit.PERCENTAGE);
		return ret;
	}
}
