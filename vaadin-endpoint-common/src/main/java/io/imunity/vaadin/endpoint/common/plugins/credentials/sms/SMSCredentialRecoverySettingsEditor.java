/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.credentials.sms;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.webui.common.FormValidationException;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

class SMSCredentialRecoverySettingsEditor
{
	private final MessageSource msg;
	private final SMSCredentialRecoverySettings initial;
	private final MessageTemplateManagement msgTplMan;
	private Checkbox enable;
	private CompatibleTemplatesComboBox emailCodeMessageTemplate;
	private IntegerField codeLength;
	private Checkbox capcha;
	
	private Binder<SMSCredentialRecoverySettings> binder;

	SMSCredentialRecoverySettingsEditor(MessageSource msg,
			MessageTemplateManagement msgTplMan, SMSCredentialRecoverySettings initial)
	{
		this.msg = msg;
		this.initial = initial;
		this.msgTplMan = msgTplMan;
	}

	public void addViewerToLayout(FormLayout parent)
	{
		Span status = new Span(
				initial.isEnabled() ? msg.getMessage("yes") : msg.getMessage("no"));
		parent.addFormItem(status, msg.getMessage("SMSCredentialRecoverSettings.enableRo"));

		if (!initial.isEnabled())
			return;
		Span emailCodeTemplate = new Span(initial.getEmailSecurityCodeMsgTemplate());
		parent.addFormItem(emailCodeTemplate, msg.getMessage("SMSCredentialRecoverSettings.emailMessageTemplate"));

		Span codeLength = new Span(String.valueOf(initial.getCodeLength()));
		parent.addFormItem(codeLength, msg.getMessage("SMSCredentialRecoverSettings.codeLengthRo"));

		Span capchaRequire = new Span(
				initial.isEnabled() ? msg.getMessage("yes") : msg.getMessage("no"));
		parent.addFormItem(capchaRequire, msg.getMessage("SMSCredentialRecoverSettings.capchaRo"));
	}

	public void addEditorToLayout(FormLayout parent)
	{
		initUI();
		setValue(initial);
		parent.addFormItem(enable, "");
		parent.addFormItem(emailCodeMessageTemplate, msg
				.getMessage("SMSCredentialRecoverSettings.emailMessageTemplate"));
		parent.addFormItem(codeLength, msg.getMessage("SMSCredentialRecoverSettings.codeLength"));
		parent.addFormItem(capcha, "");
	}

	private void setValue(SMSCredentialRecoverySettings initial)
	{
		binder.setBean(initial);
		binder.validate();
		setEnabled(initial.isEnabled());
	}

	private void initUI()
	{
		binder = new Binder<>(SMSCredentialRecoverySettings.class);
		
		enable = new Checkbox(msg.getMessage("SMSCredentialRecoverSettings.enable"));
		enable.addValueChangeListener(event -> setEnabled(enable.getValue()));
		binder.forField(enable).bind("enabled");
		
		emailCodeMessageTemplate = new CompatibleTemplatesComboBox(
				EmailPasswordResetTemplateDef.NAME, msgTplMan);
		emailCodeMessageTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(emailCodeMessageTemplate).asRequired(
				(v, c) -> ((v == null || v.isEmpty()) && (enable.getValue()))
				? ValidationResult.error(msg.getMessage("fieldRequired"))
				: ValidationResult.ok())
		.bind("emailSecurityCodeMsgTemplate");
		
		codeLength = new IntegerField();
		codeLength.setMin(1);
		codeLength.setMax(50);
		codeLength.setStepButtonsVisible(true);
		codeLength.setTitle("");
		binder.forField(codeLength).asRequired().bind("codeLength");
		
		capcha = new Checkbox(msg.getMessage("SMSCredentialRecoverSettings.capcha"));
		binder.forField(capcha).bind("capchaRequire");
	}

	private void setEnabled(Boolean value)
	{
		emailCodeMessageTemplate.setEnabled(value);
		codeLength.setEnabled(value);
		capcha.setEnabled(value);
	}

	public SMSCredentialRecoverySettings getValue() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException("");
		}

		return binder.getBean();
	}
}
