/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.confirmations;



import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor for {@link EmailConfirmationConfiguration}
 * 
 * @author P.Piernik
 *
 */
public class EmailConfirmationConfigurationEditor extends FormLayout
{
	private MessageSource msg;
	private MessageTemplateManagement msgTemplateMan;
	private Binder<EmailConfirmationConfiguration> binder;
	private EmailConfirmationConfiguration initial;
	private CompatibleTemplatesComboBox msgTemplate;
	private IntegerField validityTime;

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
		msgTemplate.setTooltipText(msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplateDesc"));

		validityTime = new IntegerField();
		validityTime.setStepButtonsVisible(true);
		
		addFieldToLayout(this);
		
		binder.forField(msgTemplate).bind("messageTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
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
	
	public void addFieldToLayout(FormLayout parent)
	{
		parent.addFormItem(msgTemplate, msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplate"));
		parent.addFormItem(validityTime, msg.getMessage("EmailConfirmationConfiguration.validityTime"));
	
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
