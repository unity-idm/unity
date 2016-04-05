/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman.layout;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * Editor of {@link FormCaptionElement} - allows for editing the capition value.
 * @author K. Benedyczak
 */
public class CaptionElementEditor extends CustomComponent implements FormElementEditor<FormCaptionElement>
{
	private FormCaptionElement element;
	private UnityMessageSource msg;
	private I18nTextField caption;
	
	public CaptionElementEditor(UnityMessageSource msg, FormCaptionElement element)
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
