/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.confirm.MobileNumberConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.FormValidationException;

public class MobileNumberConfirmationConfigurationEditor extends FormLayout
{
	private final MessageSource msg;
	private final MessageTemplateManagement msgTemplateMan;
	private Binder<MobileNumberConfirmationConfiguration> binder;
	private final MobileNumberConfirmationConfiguration initial;
	private final String msgPrefix;
	private final int defaultValidity;

	public MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
	                                                   MessageSource msg, MessageTemplateManagement msgTemplateMan, String msgPrefix)
	{
		this(initial, msg, msgTemplateMan, msgPrefix, EmailConfirmationConfiguration.DEFAULT_VALIDITY);
	}
	
	public MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
	                                                   MessageSource msg, MessageTemplateManagement msgTemplateMan, String msgPrefix, int defaultValidity)
	{
		this.initial = initial;
		this.msg = msg;
		this.msgTemplateMan = msgTemplateMan;
		this.msgPrefix = msgPrefix;
		this.defaultValidity = defaultValidity;
		initUI();	
	}
	
	public MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
	                                                   MessageSource msg, MessageTemplateManagement msgTemplateMan)
	{
		this(initial, msg, msgTemplateMan, "MobileNumberConfirmationConfiguration.");
	}
	

	private void initUI()
	{
		binder = new Binder<>(MobileNumberConfirmationConfiguration.class);

		CompatibleTemplatesComboBox msgTemplate = new CompatibleTemplatesComboBox(MobileNumberConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setLabel(msg.getMessage(
				msgPrefix + "confirmationMsgTemplate"));
		msgTemplate.setRequired(true);
		msgTemplate.setDefaultValue();

		IntegerField validityTime = new IntegerField(msg.getMessage(msgPrefix + "validityTime"));
		validityTime.setMin(1);
		validityTime.setMax(60 * 24 * 365);
		validityTime.setStep(1);
		validityTime.setWidth(4, Unit.EM);

		IntegerField codeLength = new IntegerField(msg.getMessage(msgPrefix + "codeLength"));
		codeLength.setMin(1);
		codeLength.setMax(50);
		codeLength.setStep(1);
		codeLength.setWidth(3, Unit.EM);

		add(msgTemplate);
		add(validityTime);
		add(codeLength);

		binder.forField(msgTemplate).asRequired(msg.getMessage("fieldRequired"))
				.bind("messageTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new IntegerRangeValidator(
						msg.getMessage("outOfBoundsNumber", 1, 60 * 24 * 365),
						1,
						60 * 24 * 365)
				)
				.bind("validityTime");

		binder.forField(codeLength).asRequired(msg.getMessage("fieldRequired")).withValidator(
				new IntegerRangeValidator(msg.getMessage("outOfBoundsNumber", 1, 50), 1, 50))
				.bind("codeLength");
		
		
		if (initial != null)
		{
			binder.setBean(initial);
			binder.validate();
		} else
		{
			MobileNumberConfirmationConfiguration init = new MobileNumberConfirmationConfiguration();
			init.setMessageTemplate(msgTemplate.getValue());
			init.setValidityTime(defaultValidity);
			init.setCodeLength(MobileNumberConfirmationConfiguration.DEFAULT_CODE_LENGTH);
			binder.setBean(init);
		}
		
	}

	public MobileNumberConfirmationConfiguration getCurrentValue() throws FormValidationException
	{
		binder.validate();
		if (!binder.isValid())
		{
			throw new FormValidationException("");
		}

		return binder.getBean();

	}

}
