/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.confirmations;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.msgtemplates.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor for {@link EmailConfirmationConfiguration}
 * 
 * @author P.Piernik
 *
 */
public class EmailConfirmationConfigurationEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTemplateMan;
	private Binder<EmailConfirmationConfiguration> binder;
	private EmailConfirmationConfiguration initial;
	private CompatibleTemplatesComboBox msgTemplate;
	private TextField validityTime;

	public EmailConfirmationConfigurationEditor(EmailConfirmationConfiguration initial,
			UnityMessageSource msg, MessageTemplateManagement msgTemplateMan)
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
		msgTemplate.setCaption(msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplate"));
		msgTemplate.setDescription(msg.getMessage(
				"EmailConfirmationConfiguration.confirmationMsgTemplateDesc"));
		msgTemplate.setEmptySelectionAllowed(true);

		validityTime = new TextField(
				msg.getMessage("EmailConfirmationConfiguration.validityTime"));
		
		addFieldToLayout(this);
		
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
	
	public void addFieldToLayout(Layout parent)
	{
		parent.addComponent(msgTemplate);
		parent.addComponent(validityTime);
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
