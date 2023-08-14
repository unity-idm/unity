/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.webui.common.FormValidationException;


class EmailConfirmationConfigurationEditor extends FormLayout
{
	private final MessageSource msg;
	private final MessageTemplateManagement msgTemplateMan;
	private Binder<EmailConfirmationConfiguration> binder;
	private final EmailConfirmationConfiguration initial;

	EmailConfirmationConfigurationEditor(EmailConfirmationConfiguration initial,
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

		CompatibleTemplatesComboBox msgTemplate = new CompatibleTemplatesComboBox(EmailConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setLabel(msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplate"));
		msgTemplate.setTooltipText(msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplateDesc"));
		msgTemplate.setRequired(false);

		TextField validityTime = new TextField(
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
