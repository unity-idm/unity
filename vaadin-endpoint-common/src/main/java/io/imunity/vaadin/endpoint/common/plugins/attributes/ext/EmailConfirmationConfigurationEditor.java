/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
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
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		
		binder = new Binder<>(EmailConfirmationConfiguration.class);

		CompatibleTemplatesComboBox msgTemplate = new CompatibleTemplatesComboBox(EmailConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setTooltipText(msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplateDesc"));
		msgTemplate.setRequired(false);

		IntegerField validityTime = new IntegerField();
		validityTime.setMin(1);
		validityTime.setMax(60 * 24 * 365);
		validityTime.setStepButtonsVisible(true);
		
		addFormItem(msgTemplate, msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplate"));
		addFormItem(validityTime, msg.getMessage("EmailConfirmationConfiguration.validityTime"));

		binder.forField(msgTemplate).bind("messageTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
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
