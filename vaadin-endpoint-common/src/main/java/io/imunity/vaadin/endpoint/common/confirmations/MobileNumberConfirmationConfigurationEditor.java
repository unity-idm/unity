/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.confirmations;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.endpoint.common.AttributeTypeUtils;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors.IntegerFieldWithDefaultOutOfRangeError;
import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.confirm.MobileNumberConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

public class MobileNumberConfirmationConfigurationEditor extends FormLayout
{
	private final MessageSource msg;
	private final MessageTemplateManagement msgTemplateMan;
	private Binder<MobileNumberConfirmationConfiguration> binder;
	private final MobileNumberConfirmationConfiguration initial;
	private final String msgPrefix;
	private final int defaultValidity;

	MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
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
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		binder = new Binder<>(MobileNumberConfirmationConfiguration.class);

		CompatibleTemplatesComboBox msgTemplate = new CompatibleTemplatesComboBox(MobileNumberConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setRequiredIndicatorVisible(true);
		msgTemplate.setEmptySelectionAllowed(false);
		msgTemplate.setDefaultValue();
		msgTemplate.setWidth(CSSVars.FIELD_MEDIUM.value());	

		IntegerField validityTime = new IntegerFieldWithDefaultOutOfRangeError(msg);
		validityTime.setStep(1);
		validityTime.setMax(60 * 24 * 365);
		validityTime.setMin(1);
		validityTime.setStepButtonsVisible(true);
		validityTime.setWidth(CSSVars.FIELD_MEDIUM.value());	

		
		
		IntegerField codeLength = new IntegerFieldWithDefaultOutOfRangeError(msg);
		codeLength.setStep(1);
		codeLength.setMin(1);
		codeLength.setMax(50);
		codeLength.setStepButtonsVisible(true);
		codeLength.setWidth(CSSVars.FIELD_MEDIUM.value());	

		
		addFormItem(msgTemplate, msg.getMessage(msgPrefix + "confirmationMsgTemplate"));
		addFormItem(validityTime, msg.getMessage(msgPrefix + "validityTime"));
		addFormItem(codeLength, msg.getMessage(msgPrefix + "codeLength"));

		binder.forField(msgTemplate).asRequired(msg.getMessage("fieldRequired"))
				.bind("messageTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError",
						AttributeTypeUtils.getBoundsDesc(1, 60 * 24 * 365)), 1, 60 * 24 * 365)
				)
				.bind("validityTime");

		binder.forField(codeLength).asRequired(msg.getMessage("fieldRequired")).withValidator(
				new IntegerRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError",
						AttributeTypeUtils.getBoundsDesc(1, 50)), 1, 50))
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
