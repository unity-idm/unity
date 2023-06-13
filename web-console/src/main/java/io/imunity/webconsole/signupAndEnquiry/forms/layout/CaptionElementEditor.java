/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms.layout;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * Editor of {@link FormCaptionElement} - allows for editing the capition value.
 * @author K. Benedyczak
 */
class CaptionElementEditor extends CustomComponent implements FormElementEditor<FormCaptionElement>
{
	private FormCaptionElement element;
	private MessageSource msg;
	private I18nTextField caption;
	
	CaptionElementEditor(MessageSource msg, FormCaptionElement element)
	{
		this.msg = msg;
		initUI();
		this.element = element;
		caption.setValue(this.element.getValue());
	}

	@Override
	public FormCaptionElement getElement()
	{
		return new FormCaptionElement(caption.getValue());
	}

	private void initUI()
	{
		caption = new I18nTextField(msg, msg.getMessage("CaptionElementEditor.caption"));
		FormLayout layout = new FormLayout(caption);
		setCompositionRoot(layout);
	}
}
