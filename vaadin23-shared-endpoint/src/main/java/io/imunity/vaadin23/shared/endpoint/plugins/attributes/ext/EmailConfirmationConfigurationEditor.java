/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.FormValidationException;


public class EmailConfirmationConfigurationEditor extends FormLayout
{
	private MessageSource msg;
	private MessageTemplateManagement msgTemplateMan;
	private Binder<EmailConfirmationConfiguration> binder;
	private EmailConfirmationConfiguration initial;
	private CompatibleTemplatesComboBox msgTemplate;
	private TextField validityTime;

	public EmailConfirmationConfigurationEditor(EmailConfirmationConfiguration initial,
	                                            MessageSource msg, MessageTemplateManagement msgTemplateMan)
	{
		this.initial = initial;
		this.msg = msg;
		this.msgTemplateMan = msgTemplateMan;
		initUI();
	}

	private void initUI()
	{
		binder = new Binder<>(EmailConfirmationConfiguration.class);
		
		msgTemplate = new CompatibleTemplatesComboBox(EmailConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setLabel(msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplate"));
		msgTemplate.getElement().setProperty("title", msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplateDesc"));
		msgTemplate.setRequired(false);

		validityTime = new TextField(
				msg.getMessage("EmailConfirmationConfiguration.validityTime"));

		add(msgTemplate);
		add(validityTime);

		binder.forField(msgTemplate).bind("messageTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("notAnIntNumber")))
				.withValidator(new IntegerRangeValidator(msg
						.getMessage("outOfBoundsNumber", 1, 60 * 24 * 365),
						1, 60 * 24 * 365))
				.bind("validityTime");

		if (initial != null)
		{
			binder.setBean(initial);
		} else
		{
			EmailConfirmationConfiguration init = new EmailConfirmationConfiguration();
			init.setMessageTemplate(msgTemplate.getValue());
			init.setValidityTime(EmailConfirmationConfiguration.DEFAULT_VALIDITY);
			binder.setBean(init);
		}
		
	}

	public EmailConfirmationConfiguration getCurrentValue() throws FormValidationException
	{
		binder.validate();
		if (!binder.isValid())
		{
			throw new FormValidationException("");
		}

		return binder.getBean();
	}

}