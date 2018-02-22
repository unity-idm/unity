/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.confirmations;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.msgtemplates.confirm.MobileNumberConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;

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
	private TextField validityTime;
	private TextField codeLenght;

	public MobileNumberConfirmationConfigurationEditor(MobileNumberConfirmationConfiguration initial,
			UnityMessageSource msg, MessageTemplateManagement msgTemplateMan)
	{
		super();
		this.initial = initial;
		this.msg = msg;
		this.msgTemplateMan = msgTemplateMan;
		initUI();
	}

	private void initUI()
	{
		binder = new Binder<>(MobileNumberConfirmationConfiguration.class);
		
		msgTemplate = new CompatibleTemplatesComboBox(MobileNumberConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setCaption(msg.getMessage(
				"MobileNumberConfirmationConfiguration.confirmationMsgTemplate"));
		msgTemplate.setEmptySelectionAllowed(false);
		msgTemplate.setDefaultValue();

		validityTime = new TextField(
				msg.getMessage("MobileNumberConfirmationConfiguration.validityTime"));
		
		codeLenght = new TextField(
				msg.getMessage("MobileNumberConfirmationConfiguration.codeLenght"));
		
		
		addFieldToLayout(this);
		
		binder.forField(msgTemplate).asRequired(msg.getMessage("fieldRequired"))
				.bind("messageTemplate");
		binder.forField(validityTime).asRequired(msg.getMessage("fieldRequired"))
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("notAnIntNumber")))
				.withValidator(new IntegerRangeValidator(msg
						.getMessage("outOfBoundsNumber", 1, 60 * 24 * 365),
						1, 60 * 24 * 365))
				.bind("validityTime");

		binder.forField(codeLenght).asRequired(msg.getMessage("fieldRequired"))
		.withConverter(new StringToIntegerConverter(
				msg.getMessage("notAnIntNumber")))
		.withValidator(new IntegerRangeValidator(msg
				.getMessage("outOfBoundsNumber", 1, 50),
				1, 50))
		.bind("codeLenght");
		
		
		if (initial != null)
		{
			binder.setBean(initial);
		} else
		{
			MobileNumberConfirmationConfiguration init = new MobileNumberConfirmationConfiguration();
			init.setMessageTemplate(msgTemplate.getValue());
			init.setValidityTime(EmailConfirmationConfiguration.DEFAULT_VALIDITY);
			init.setCodeLenght(MobileNumberConfirmationConfiguration.DEFAULT_CODE_LENGHT);
			binder.setBean(init);
		}
		
	}
	
	public void addFieldToLayout(Layout parent)
	{
		parent.addComponent(msgTemplate);
		parent.addComponent(validityTime);
		parent.addComponent(codeLenght);
	}

	public MobileNumberConfirmationConfiguration getCurrentValue() throws IllegalAttributeTypeException
	{
		if (!binder.isValid())
		{
			binder.validate();
			throw new IllegalAttributeTypeException("");
		}

		return binder.getBean();

	}

}
