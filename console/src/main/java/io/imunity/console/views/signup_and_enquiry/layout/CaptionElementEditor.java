/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.layout;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.layout.FormCaptionElement;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

/**
 * Editor of {@link FormCaptionElement} - allows for editing the capition value.
 * @author K. Benedyczak
 */
class CaptionElementEditor extends VerticalLayout implements FormElementEditor<FormCaptionElement>
{
	private final MessageSource msg;
	private LocalizedTextFieldDetails caption;
	
	CaptionElementEditor(MessageSource msg, FormCaptionElement element)
	{
		this.msg = msg;
		initUI();
		caption.setValue(element.getValue().getLocalizedMap());
	}

	@Override
	public FormCaptionElement getComponent()
	{
		return new FormCaptionElement(new I18nString(caption.getValue()));
	}

	private void initUI()
	{
		setPadding(false);
		caption = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		caption.setWidth(TEXT_FIELD_MEDIUM.value());
		FormLayout layout = new FormLayout();
		layout.addFormItem(caption, msg.getMessage("CaptionElementEditor.caption"))
				.getStyle().set("--vaadin-form-item-label-width", "4em");
		add(layout);
	}
}
