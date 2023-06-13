/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.credentials.sms;

import java.util.Optional;

import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.sms.SMSAuthnTemplateDef;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredential;
import pl.edu.icm.unity.stdext.credential.sms.SMSVerificator;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.confirmations.MobileNumberConfirmationConfigurationEditor;

/**
 * {@link CredentialDefinition} editor and viewer for the {@link SMSVerificator}.
 * @author P. Piernik
 */
public class SMSCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private MessageSource msg;
	private MessageTemplateManagement msgTplMan;
	
	private IntStepper codeLength;
	private IntStepper validityTime;
	private IntStepper authSMSLimit;
	private CompatibleTemplatesComboBox msgTemplate;
	private SMSCredentialRecoverySettingsEditor recoverySettings;
	private MobileNumberConfirmationConfigurationEditor confirmationConfigEditor;
	private Binder<SMSCredential> binder;
	
	public SMSCredentialDefinitionEditor(MessageSource msg, MessageTemplateManagement msgTplMan)
	{
		this.msg = msg;
		this.msgTplMan = msgTplMan;
	}

	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{	
		SMSCredential helper = new SMSCredential();
		helper.setSerializedConfiguration(JsonUtil.parse(credentialDefinitionConfiguration));
		
		Label msgTemplate = new Label();
		msgTemplate.setCaption(msg.getMessage("SMSCredentialDefinitionEditor.msgTemplate"));
		msgTemplate.setValue(helper.getMessageTemplate());
		
		Label codeLength = new Label();
		codeLength.setCaption(msg.getMessage("SMSCredentialDefinitionEditor.codeLength"));
		codeLength.setValue(String.valueOf(helper.getCodeLength()));
		
		Label validityTime = new Label();
		validityTime.setCaption(msg.getMessage("SMSCredentialDefinitionEditor.validityTime"));
		validityTime.setValue(String.valueOf(helper.getValidityTime()));
		
		Label smsLimit = new Label();
		smsLimit.setCaption(msg.getMessage("SMSCredentialDefinitionEditor.smsLimit"));
		smsLimit.setValue(String.valueOf(helper.getAuthnSMSLimit()));
		
		FormLayout form = new CompactFormLayout(msgTemplate, codeLength, validityTime, smsLimit, HtmlTag.br());	
		
		SMSCredentialRecoverySettingsEditor viewer = new SMSCredentialRecoverySettingsEditor(msg, msgTplMan,
				helper.getRecoverySettings());
		viewer.addViewerToLayout(form);
		form.addComponent(HtmlTag.br());
	
		Optional<MobileNumberConfirmationConfiguration> config = helper.
				getMobileNumberConfirmationConfiguration();
		Label nmsgTemplate = new Label();
		nmsgTemplate.setCaption(msg.getMessage(
				"SMSCredentialDefinitionEditor.newMobile.confirmationMsgTemplate"));
		form.addComponent(nmsgTemplate);
		Label nvalidityTime = new Label();
		nvalidityTime.setCaption(msg.getMessage(
				"SMSCredentialDefinitionEditor.newMobile.validityTime"));
		form.addComponent(nvalidityTime);
		Label ncodeLength = new Label();
		ncodeLength.setCaption(msg.getMessage(
				"SMSCredentialDefinitionEditor.newMobile.codeLength"));
		form.addComponent(ncodeLength);	
		nmsgTemplate.setValue(
				config.isPresent() ? config.get().getMessageTemplate() : "");

		nvalidityTime.setValue(config.isPresent()? String.valueOf(config.get().getValidityTime()) : "");
		ncodeLength.setValue(config.isPresent() ? String.valueOf(config.get().getCodeLength()) : "");
			
		form.setMargin(true);	
		return form;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		binder = new Binder<>(SMSCredential.class);
		
		msgTemplate = new CompatibleTemplatesComboBox(SMSAuthnTemplateDef.NAME, msgTplMan);
		msgTemplate.setCaption(msg.getMessage(
				"SMSCredentialDefinitionEditor.msgTemplate"));
		msgTemplate.setEmptySelectionAllowed(false);
		binder.forField(msgTemplate).asRequired().bind("messageTemplate");
		
		codeLength = new IntStepper(
				msg.getMessage("SMSCredentialDefinitionEditor.codeLength"));
		codeLength.setMinValue(1);
		codeLength.setMaxValue(50);
		codeLength.setWidth(3, Unit.EM);
		binder.forField(codeLength).asRequired().bind("codeLength");
		
		validityTime = new IntStepper(msg.getMessage("SMSCredentialDefinitionEditor.validityTime"));
		validityTime.setMinValue(1);
		validityTime.setMaxValue(525600);
		validityTime.setWidth(4, Unit.EM);
		binder.forField(validityTime).asRequired().bind("validityTime");
		
		authSMSLimit = new IntStepper(msg.getMessage("SMSCredentialDefinitionEditor.smsLimit"));
		authSMSLimit.setMinValue(1);
		authSMSLimit.setMaxValue(10000);
		authSMSLimit.setWidth(4, Unit.EM);
		binder.forField(authSMSLimit).asRequired().bind("authnSMSLimit");		
		
		FormLayout form = new CompactFormLayout(msgTemplate, codeLength, validityTime, authSMSLimit, HtmlTag.br());
		form.setSpacing(true);
		form.setMargin(true);

		SMSCredential helper = new SMSCredential();
		
		if (credentialDefinitionConfiguration != null)
		{	helper.setSerializedConfiguration(
					JsonUtil.parse(credentialDefinitionConfiguration));
			binder.setBean(helper);
			binder.validate();
		}else
		{
			binder.setBean(helper);
		}
	
		recoverySettings = new SMSCredentialRecoverySettingsEditor(msg, msgTplMan,
				helper.getRecoverySettings());
		recoverySettings.addEditorToLayout(form);
		form.addComponent(HtmlTag.br());
		
		MobileNumberConfirmationConfiguration confirmationConfig = null;
		if (helper != null && helper.getMobileNumberConfirmationConfiguration().isPresent())
		{	confirmationConfig = helper.getMobileNumberConfirmationConfiguration()
					.get();
		}
		confirmationConfigEditor = new MobileNumberConfirmationConfigurationEditor(
				confirmationConfig, msg, msgTplMan,
				"SMSCredentialDefinitionEditor.newMobile.",
				SMSCredential.DEFAULT_VALIDITY);

		confirmationConfigEditor.addFieldToLayout(form);
		
		return form;
	}

	@Override
	public String getCredentialDefinition() throws IllegalCredentialException
	{
		if (binder.validate().hasErrors())
			throw new IllegalCredentialException("", new FormValidationException());	
		SMSCredential helper = binder.getBean();
		try
		{
			helper.setRecoverySettings(recoverySettings.getValue());
			helper.setMobileNumberConfirmationConfiguration(confirmationConfigEditor.getCurrentValue());
		} catch (FormValidationException e)
		{
			throw new IllegalCredentialException("", e);
		}
		return JsonUtil.serialize(helper.getSerializedConfiguration());
	}
}
