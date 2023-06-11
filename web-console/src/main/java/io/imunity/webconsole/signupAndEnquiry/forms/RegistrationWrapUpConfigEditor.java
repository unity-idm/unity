/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.function.Predicate;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

public class RegistrationWrapUpConfigEditor extends CustomComponent
{
	private EnumComboBox<TriggeringState> trigger;
	private I18nTextField title;
	private I18nTextField info;
	private TextField redirectURL;
	private IntStepper redirectAfter;
	private CheckBox automatic;
	private I18nTextField redirectCaption;
	private MessageSource msg;
	
	public RegistrationWrapUpConfigEditor(MessageSource msg, 
			Predicate<RegistrationWrapUpConfig.TriggeringState> filter)
	{
		this.msg = msg;
		FormLayout layout = new CompactFormLayout();
		title = new I18nTextField(msg, msg.getMessage("RegistrationFormEditor.wrapupTitle"));
		info = new I18nTextField(msg, msg.getMessage("RegistrationFormEditor.wrapupInfo"));
		info.setWidth(100, Unit.PERCENTAGE);
		redirectURL = new TextField(msg.getMessage("RegistrationFormEditor.wrapupRedirect"));
		redirectURL.setWidth(100, Unit.PERCENTAGE);
		redirectAfter = new IntStepper(msg.getMessage("RegistrationFormEditor.wrapupRedirectAfter"));
		redirectAfter.setWidth(3, Unit.EM);
		redirectCaption = new I18nTextField(msg, msg.getMessage("RegistrationFormEditor.wrapupRedirectCaption"));
		automatic = new CheckBox(msg.getMessage("RegistrationFormEditor.automaticRedirect"));
		trigger = new EnumComboBox<RegistrationWrapUpConfig.TriggeringState>(
				msg.getMessage("RegistrationFormEditor.wrapupWhen"), msg, null, 
				TriggeringState.class, TriggeringState.DEFAULT, filter);
		automatic.addValueChangeListener(e -> setState());
		layout.addComponents(trigger, title, info, redirectURL, automatic, redirectCaption, redirectAfter);
		setCompositionRoot(layout);
	}
	
	public void setValue(RegistrationWrapUpConfig toEdit)
	{
		if (toEdit == null)
			return;
		if (toEdit.getTitle() != null)
			title.setValue(toEdit.getTitle());
		if (toEdit.getInfo() != null)
			info.setValue(toEdit.getInfo());
		if (toEdit.getRedirectCaption() != null)
			redirectCaption.setValue(toEdit.getRedirectCaption());
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
		

		return new RegistrationWrapUpConfig(trigger.getValue(), title.getValue(), info.getValue(), 
				redirectCaption.getValue(), 
				automatic.getValue(), redirectURL.getValue(), Duration.ofSeconds(redirectAfter.getValue()));
	}
}
