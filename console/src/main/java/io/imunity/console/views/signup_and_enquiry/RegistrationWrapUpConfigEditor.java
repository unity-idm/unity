/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.EnumComboBox;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.function.Predicate;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

public class RegistrationWrapUpConfigEditor extends VerticalLayout
{
	private final ComboBox<TriggeringState> trigger;
	private final LocalizedTextFieldDetails title;
	private final LocalizedTextFieldDetails info;
	private final TextField redirectURL;
	private final IntegerField redirectAfter;
	private final Checkbox automatic;
	private final LocalizedTextFieldDetails redirectCaption;
	private final MessageSource msg;
	
	public RegistrationWrapUpConfigEditor(MessageSource msg, 
			Predicate<TriggeringState> filter)
	{
		setPadding(false);
		this.msg = msg;
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		title = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		title.setWidth(TEXT_FIELD_MEDIUM.value());
		info = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		info.setWidth(TEXT_FIELD_BIG.value());
		redirectURL = new TextField();
		redirectURL.setWidth(TEXT_FIELD_BIG.value());
		redirectAfter = new IntegerField();
		redirectAfter.setStepButtonsVisible(true);
		redirectAfter.setMin(0);
		redirectAfter.addValueChangeListener(e -> {
			if(e.getValue() == null)
				redirectAfter.setValue(e.getOldValue());
		});
		redirectAfter.setValue(0);
		redirectCaption = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		redirectCaption.setWidth(TEXT_FIELD_MEDIUM.value());
		automatic = new Checkbox(msg.getMessage("RegistrationFormEditor.automaticRedirect"));
		trigger = new EnumComboBox<>(
				null, msg::getMessage, null,
				TriggeringState.class, TriggeringState.DEFAULT, filter);
		trigger.setWidth(TEXT_FIELD_MEDIUM.value());
		automatic.addValueChangeListener(e -> setState());
		layout.addFormItem(trigger, msg.getMessage("RegistrationFormEditor.wrapupWhen"));
		layout.addFormItem(title, msg.getMessage("RegistrationFormEditor.wrapupTitle"));
		layout.addFormItem(info, msg.getMessage("RegistrationFormEditor.wrapupInfo"));
		layout.addFormItem(redirectURL, msg.getMessage("RegistrationFormEditor.wrapupRedirect"));
		layout.addFormItem(automatic, "");
		layout.addFormItem(redirectCaption, msg.getMessage("RegistrationFormEditor.wrapupRedirectCaption"));
		layout.addFormItem(redirectAfter, msg.getMessage("RegistrationFormEditor.wrapupRedirectAfter"));
		add(layout);
	}
	
	public void setValue(RegistrationWrapUpConfig toEdit)
	{
		if (toEdit == null)
			return;
		if (toEdit.getTitle() != null)
			title.setValue(toEdit.getTitle().getLocalizedMap());
		if (toEdit.getInfo() != null)
			info.setValue(toEdit.getInfo().getLocalizedMap());
		if (toEdit.getRedirectCaption() != null)
			redirectCaption.setValue(toEdit.getRedirectCaption().getLocalizedMap());
		if (toEdit.getRedirectURL() != null)
			redirectURL.setValue(toEdit.getRedirectURL());
		if (toEdit.getRedirectAfterTime() != null)
			redirectAfter.setValue(Long.valueOf(toEdit.getRedirectAfterTime().getSeconds()).intValue());
		trigger.setValue(toEdit.getState());
		automatic.setValue(toEdit.isAutomatic());
		setState();
	}
	
	private void setState()
	{
		redirectCaption.setEnabled(!automatic.getValue());
		redirectAfter.setEnabled(!automatic.getValue());
		title.setEnabled(!automatic.getValue());
		info.setEnabled(!automatic.getValue());
	}
	
	public RegistrationWrapUpConfig getValue() throws FormValidationException
	{
		try
		{
			new URI(redirectURL.getValue());
		} catch (URISyntaxException e)
		{
			throw new FormValidationException(msg.getMessage("RegistrationFormEditor.invalidRedirectURL", trigger.getValue().name()));
		}
		
		if (redirectAfter.getValue() < 0)
		{
			throw new FormValidationException(msg.getMessage("RegistrationFormEditor.invalidRedirectAfter", trigger.getValue().name()));
		}
		

		return new RegistrationWrapUpConfig(trigger.getValue(),
				new I18nString(title.getValue()), new I18nString(info.getValue()), new I18nString(redirectCaption.getValue()),
				automatic.getValue(), redirectURL.getValue(), Duration.ofSeconds(redirectAfter.getValue()));
	}
}
