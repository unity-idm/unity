/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.credentials.sms;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Part of UI responsible for lost phone recovery settings
 * @author P.Piernik
 *
 */
public class SMSCredentialRecoverySettingsEditor
{
	
	private MessageSource msg;
	private SMSCredentialRecoverySettings initial;
	private CheckBox enable;
	private MessageTemplateManagement msgTplMan;
	private CompatibleTemplatesComboBox emailCodeMessageTemplate;
	private IntStepper codeLength;
	private CheckBox capcha;
	
	private Binder<SMSCredentialRecoverySettings> binder;
	
	public SMSCredentialRecoverySettingsEditor(MessageSource msg,
			MessageTemplateManagement msgTplMan)
	{
		this(msg, msgTplMan, new SMSCredentialRecoverySettings());
	}

	public SMSCredentialRecoverySettingsEditor(MessageSource msg,
			MessageTemplateManagement msgTplMan, SMSCredentialRecoverySettings initial)
	{
		this.msg = msg;
		this.initial = initial;
		this.msgTplMan = msgTplMan;
	}

	public void addViewerToLayout(FormLayout parent)
	{
		Label status = new Label(
				initial.isEnabled() ? msg.getMessage("yes") : msg.getMessage("no"));
		status.setCaption(msg.getMessage("SMSCredentialRecoverSettings.enableRo"));
		parent.addComponent(status);
		if (!initial.isEnabled())
			return;
		Label emailCodeTemplate = new Label(initial.getEmailSecurityCodeMsgTemplate());
		emailCodeTemplate.setCaption(msg
				.getMessage("SMSCredentialRecoverSettings.emailMessageTemplate"));

		Label codeLength = new Label(String.valueOf(initial.getCodeLength()));
		codeLength.setCaption(msg.getMessage("SMSCredentialRecoverSettings.codeLengthRo"));

		Label capchaRequire = new Label(
				initial.isEnabled() ? msg.getMessage("yes") : msg.getMessage("no"));
		capchaRequire.setCaption(msg.getMessage("SMSCredentialRecoverSettings.capchaRo"));
		parent.addComponents(emailCodeTemplate, codeLength, capchaRequire);
	}

	public void addEditorToLayout(FormLayout parent)
	{
		initUI();
		setValue(initial);
		parent.addComponents(enable, emailCodeMessageTemplate, codeLength, capcha);
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
		
		enable = new CheckBox(msg.getMessage("SMSCredentialRecoverSettings.enable"));
		enable.addValueChangeListener(event -> setEnabled(enable.getValue()));
		binder.forField(enable).bind("enabled");
		
		emailCodeMessageTemplate = new CompatibleTemplatesComboBox(
				EmailPasswordResetTemplateDef.NAME, msgTplMan);
		emailCodeMessageTemplate.setCaption(msg
				.getMessage("SMSCredentialRecoverSettings.emailMessageTemplate"));
		emailCodeMessageTemplate.setEmptySelectionAllowed(false);
		binder.forField(emailCodeMessageTemplate).asRequired(
				(v, c) -> ((v == null || v.isEmpty()) && (enable.getValue()))
				? ValidationResult.error(msg.getMessage("fieldRequired"))
				: ValidationResult.ok())
		.bind("emailSecurityCodeMsgTemplate");
		
		codeLength = new IntStepper(
				msg.getMessage("SMSCredentialRecoverSettings.codeLength"));
		codeLength.setMinValue(1);
		codeLength.setMaxValue(50);
		codeLength.setWidth(3, Unit.EM);
		binder.forField(codeLength).bind("codeLength");
		
		capcha = new CheckBox(msg.getMessage("SMSCredentialRecoverSettings.capcha"));
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
