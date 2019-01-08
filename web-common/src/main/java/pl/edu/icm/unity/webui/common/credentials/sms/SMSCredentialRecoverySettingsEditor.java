/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.credentials.sms;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;

/**
 * Part of UI responsible for lost phone recovery settings
 * @author P.Piernik
 *
 */
public class SMSCredentialRecoverySettingsEditor
{
	
	private UnityMessageSource msg;
	private SMSCredentialRecoverySettings initial;
	private CheckBox enable;
	private MessageTemplateManagement msgTplMan;
	private CompatibleTemplatesComboBox emailCodeMessageTemplate;
	private IntStepper codeLength;
	private CheckBox capcha;
	
	public SMSCredentialRecoverySettingsEditor(UnityMessageSource msg,
			MessageTemplateManagement msgTplMan)
	{
		this(msg, msgTplMan, new SMSCredentialRecoverySettings());
	}

	public SMSCredentialRecoverySettingsEditor(UnityMessageSource msg,
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
		enable.setValue(initial.isEnabled());
		emailCodeMessageTemplate.setValue(initial.getEmailSecurityCodeMsgTemplate());
		codeLength.setValue(initial.getCodeLength());
		capcha.setValue(initial.isCapchaRequired());
		setEnabled(initial.isEnabled());
	}

	private void initUI()
	{
		enable = new CheckBox(msg.getMessage("SMSCredentialRecoverSettings.enable"));
		enable.addValueChangeListener(event -> setEnabled(enable.getValue()));
		emailCodeMessageTemplate = new CompatibleTemplatesComboBox(
				EmailPasswordResetTemplateDef.NAME, msgTplMan);
		emailCodeMessageTemplate.setCaption(msg
				.getMessage("SMSCredentialRecoverSettings.emailMessageTemplate"));
		emailCodeMessageTemplate.setEmptySelectionAllowed(false);
		codeLength = new IntStepper(
				msg.getMessage("SMSCredentialRecoverSettings.codeLength"));
		codeLength.setMinValue(1);
		codeLength.setMaxValue(50);
		codeLength.setWidth(3, Unit.EM);
		capcha = new CheckBox(msg.getMessage("SMSCredentialRecoverSettings.capcha"));
	}

	private void setEnabled(Boolean value)
	{
		emailCodeMessageTemplate.setEnabled(value);
		codeLength.setEnabled(value);
		capcha.setEnabled(value);
	}

	public SMSCredentialRecoverySettings getValue()
	{
		SMSCredentialRecoverySettings ret = new SMSCredentialRecoverySettings();
		ret.setEnabled(enable.getValue());
		if (emailCodeMessageTemplate.getValue() != null)
			ret.setEmailSecurityCodeMsgTemplate(
					emailCodeMessageTemplate.getValue().toString());
		ret.setCapchaRequire(capcha.getValue());
		ret.setCodeLength(codeLength.getValue());
		return ret;
	}
}
