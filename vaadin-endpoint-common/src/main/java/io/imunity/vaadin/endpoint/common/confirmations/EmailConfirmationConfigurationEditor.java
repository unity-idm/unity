/* Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.confirmations;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors.IntegerFieldWithDefaultOutOfRangeError;
import pl.edu.icm.unity.base.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.webui.common.AttributeTypeUtils;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor for {@link EmailConfirmationConfiguration}
 * 
 * @author P.Piernik
 *
 */
public class EmailConfirmationConfigurationEditor extends FormLayout
{
	private final MessageSource msg;
	private final MessageTemplateManagement msgTemplateMan;
	private Binder<EmailConfirmationConfiguration> binder;
	private final EmailConfirmationConfiguration initial;
	private CompatibleTemplatesComboBox msgTemplate;
	private IntegerField validityTime;

	public EmailConfirmationConfigurationEditor(EmailConfirmationConfiguration initial, MessageSource msg,
			MessageTemplateManagement msgTemplateMan)
	{
		this.initial = initial;
		this.msg = msg;
		this.msgTemplateMan = msgTemplateMan;
		initUI();
	}

	private void initUI()
	{
		setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		
		binder = new Binder<>(EmailConfirmationConfiguration.class);

		msgTemplate = new CompatibleTemplatesComboBox(EmailConfirmationTemplateDef.NAME, msgTemplateMan);
		msgTemplate.setTooltipText(msg.getMessage("EmailConfirmationConfiguration.confirmationMsgTemplateDesc"));
		msgTemplate.setWidth(CSSVars.FIELD_MEDIUM.value());	
		
		validityTime = new IntegerFieldWithDefaultOutOfRangeError(msg);
		validityTime.setStepButtonsVisible(true);
		validityTime.setMax(60 * 24 * 365);
		validityTime.setMin(1);
		validityTime.setWidth(CSSVars.FIELD_MEDIUM.value());
		
		addFieldToLayout(this);

		binder.forField(msgTemplate)
				.bind("messageTemplate");
		binder.forField(validityTime)
				.asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new IntegerRangeValidator(msg.getMessage("NumericAttributeHandler.rangeError",
						AttributeTypeUtils.getBoundsDesc(1, 60 * 24 * 365)), 1, 60 * 24 * 365))
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
		parent.addFormItem(msgTemplate, msg.getMessage("EmailConfirmationConfiguration.confirmationMsgTemplate"));
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
