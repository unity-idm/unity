/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.confirmations;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.Layout;

import pl.edu.icm.unity.base.msgtemplates.confirm.MobileNumberConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor for {@link MobileNumberConfirmationConfiguration}
 * 
 * @author P.Piernik
 *
 */
public class MobileNumberConfirmationConfigurationEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTemplateMan;
	private Binder<MobileNumberConfirmationConfiguration> binder;
	private MobileNumberConfirmationConfiguration initial;
	private CompatibleTemplatesComboBox msgTemplate;
	private IntStepper validityTime;
	private IntStepper codeLength;
	private String msgPrefix;
	private int defaultValidity;

	public MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
			UnityMessageSource msg, MessageTemplateManagement msgTemplateMan, String msgPrefix)
	{
		this(initial, msg, msgTemplateMan, msgPrefix, EmailConfirmationConfiguration.DEFAULT_VALIDITY);
	}
	
	public MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
			UnityMessageSource msg, MessageTemplateManagement msgTemplateMan, String msgPrefix, int defaultValidity)
	{
		this.initial = initial;
		this.msg = msg;
		this.msgTemplateMan = msgTemplateMan;
		this.msgPrefix = msgPrefix;
		this.defaultValidity = defaultValidity;
		initUI();	
	}
	
	public MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
			UnityMessageSource msg, MessageTemplateManagement msgTemplateMan)
	{
		this(initial, msg, msgTemplateMan, "MobileNumberConfirmationConfiguration.");
	}
	

	private void initUI()
	{
		binder = new Binder<>(MobileNumberConfirmationConfiguration.class);
		
		msgTemplate = new CompatibleTemplatesComboBox(MobileNumberConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setCaption(msg.getMessage(
				msgPrefix + "confirmationMsgTemplate"));
		msgTemplate.setEmptySelectionAllowed(false);
		msgTemplate.setDefaultValue();

		
		validityTime = new IntStepper(msg.getMessage(msgPrefix + "validityTime"));
		validityTime.setMinValue(1);
		validityTime.setMaxValue(60 * 24 * 365);
		validityTime.setWidth(4, Unit.EM);
		
		codeLength = new IntStepper(
				msg.getMessage(msgPrefix + "codeLength"));
		codeLength.setMinValue(1);
		codeLength.setMaxValue(50);
		codeLength.setWidth(3, Unit.EM);
		
		addFieldToLayout(this);
		
		binder.forField(msgTemplate).asRequired(msg.getMessage("fieldRequired"))
				.bind("messageTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new IntegerRangeValidator(msg
						.getMessage("outOfBoundsNumber", 1, 60 * 24 * 365),
						1, 60 * 24 * 365))
				.bind("validityTime");

		binder.forField(codeLength).asRequired(msg.getMessage("fieldRequired"))
		.withValidator(new IntegerRangeValidator(msg
				.getMessage("outOfBoundsNumber", 1, 50),
				1, 50))
		.bind("codeLength");
		
		
		if (initial != null)
		{
			binder.setBean(initial);
		} else
		{
			MobileNumberConfirmationConfiguration init = new MobileNumberConfirmationConfiguration();
			init.setMessageTemplate(msgTemplate.getValue());
			init.setValidityTime(defaultValidity);
			init.setCodeLength(MobileNumberConfirmationConfiguration.DEFAULT_CODE_LENGTH);
			binder.setBean(init);
		}
		
	}
	
	public void addFieldToLayout(Layout parent)
	{
		parent.addComponent(msgTemplate);
		parent.addComponent(validityTime);
		parent.addComponent(codeLength);
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
