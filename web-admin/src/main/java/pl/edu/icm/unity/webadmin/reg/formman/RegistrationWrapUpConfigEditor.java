/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Predicate;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
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
	private CheckBox automatic;
	private I18nTextField redirectCaption;
	private UnityMessageSource msg;
	
	public RegistrationWrapUpConfigEditor(UnityMessageSource msg, 
			Predicate<RegistrationWrapUpConfig.TriggeringState> filter)
	{
		this.msg = msg;
		FormLayout layout = new CompactFormLayout();
		title = new I18nTextField(msg, msg.getMessage("RegistrationFormEditor.wrapupTitle"));
		info = new I18nTextField(msg, msg.getMessage("RegistrationFormEditor.wrapupInfo"));
		info.setWidth(100, Unit.PERCENTAGE);
		redirectURL = new TextField(msg.getMessage("RegistrationFormEditor.wrapupRedirect"));
		redirectURL.setWidth(100, Unit.PERCENTAGE);
		redirectCaption = new I18nTextField(msg, msg.getMessage("RegistrationFormEditor.wrapupRedirectCaption"));
		automatic = new CheckBox(msg.getMessage("RegistrationFormEditor.automaticRedirect"));
		trigger = new EnumComboBox<RegistrationWrapUpConfig.TriggeringState>(
				msg.getMessage("RegistrationFormEditor.wrapupWhen"), msg, null, 
				TriggeringState.class, TriggeringState.DEFAULT, filter);
		automatic.addValueChangeListener(e -> setState());
		layout.addComponents(trigger, title, info, redirectURL, automatic, redirectCaption);
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
		trigger.setValue(toEdit.getState());
		automatic.setValue(toEdit.isAutomatic());
		setState();
	}
	
	private void setState()
	{
		redirectCaption.setEnabled(!automatic.getValue());
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

		return new RegistrationWrapUpConfig(trigger.getValue(), title.getValue(), info.getValue(), 
				redirectCaption.getValue(), 
				automatic.getValue(), redirectURL.getValue());
	}
}
