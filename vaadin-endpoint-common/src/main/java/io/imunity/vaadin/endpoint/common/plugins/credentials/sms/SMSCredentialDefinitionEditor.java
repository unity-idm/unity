/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.sms;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ext.MobileNumberConfirmationConfigurationEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.base.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.sms.SMSAuthnTemplateDef;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredential;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Optional;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

public class SMSCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private final MessageSource msg;
	private final MessageTemplateManagement msgTplMan;

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

		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		formLayout.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());

		Span msgTemplate = new Span(helper.getMessageTemplate());
		formLayout.addFormItem(msgTemplate, msg.getMessage("SMSCredentialDefinitionEditor.msgTemplate"));

		Span codeLength = new Span(String.valueOf(helper.getCodeLength()));
		formLayout.addFormItem(codeLength, msg.getMessage("SMSCredentialDefinitionEditor.codeLength"));

		Span validityTime = new Span(String.valueOf(helper.getValidityTime()));
		formLayout.addFormItem(validityTime, msg.getMessage("SMSCredentialDefinitionEditor.validityTime"));
		
		Span smsLimit = new Span(String.valueOf(helper.getAuthnSMSLimit()));
		formLayout.addFormItem(smsLimit, msg.getMessage("SMSCredentialDefinitionEditor.smsLimit"));

		formLayout.add(new Hr());
		
		SMSCredentialRecoverySettingsEditor viewer = new SMSCredentialRecoverySettingsEditor(msg, msgTplMan,
				helper.getRecoverySettings());
		viewer.addViewerToLayout(formLayout);
		formLayout.add(new Hr());
	
		Optional<MobileNumberConfirmationConfiguration> config = helper.
				getMobileNumberConfirmationConfiguration();
		Span nmsgTemplate = new Span();
		formLayout.addFormItem(nmsgTemplate, msg.getMessage(
				"SMSCredentialDefinitionEditor.newMobile.confirmationMsgTemplate"));
		Span nvalidityTime = new Span();
		formLayout.addFormItem(nvalidityTime, msg.getMessage(
				"SMSCredentialDefinitionEditor.newMobile.validityTime"));
		Span ncodeLength = new Span();
		formLayout.addFormItem(ncodeLength, msg.getMessage(
				"SMSCredentialDefinitionEditor.newMobile.codeLength"));
		nmsgTemplate.setText(
				config.isPresent() ? config.get().getMessageTemplate() : "");

		nvalidityTime.setText(config.map(mobileNumberConfirmationConfiguration -> String.valueOf(
				mobileNumberConfirmationConfiguration.getValidityTime())).orElse(""));
		ncodeLength.setText(config.map(mobileNumberConfirmationConfiguration -> String.valueOf(
				mobileNumberConfirmationConfiguration.getCodeLength())).orElse(""));
			
		return formLayout;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		binder = new Binder<>(SMSCredential.class);

		FormLayout formLayout = new FormLayout();
		formLayout.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		CompatibleTemplatesComboBox msgTemplate = new CompatibleTemplatesComboBox(SMSAuthnTemplateDef.NAME, msgTplMan);
		formLayout.addFormItem(msgTemplate, msg.getMessage("SMSCredentialDefinitionEditor.msgTemplate"));
		msgTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(msgTemplate).asRequired().bind("messageTemplate");

		IntegerField codeLength = new IntegerField();
		codeLength.setMin(3);
		codeLength.setMax(50);
		codeLength.setStepButtonsVisible(true);
		formLayout.addFormItem(codeLength, msg.getMessage("SMSCredentialDefinitionEditor.codeLength"));
		binder.forField(codeLength).asRequired().bind("codeLength");

		IntegerField validityTime = new IntegerField();
		validityTime.setMin(1);
		validityTime.setMax(525600);
		validityTime.setStepButtonsVisible(true);
		formLayout.addFormItem(validityTime, msg.getMessage("SMSCredentialDefinitionEditor.validityTime"));
		binder.forField(validityTime).asRequired().bind("validityTime");

		IntegerField authSMSLimit = new IntegerField();
		authSMSLimit.setMin(1);
		authSMSLimit.setMax(10000);
		authSMSLimit.setStepButtonsVisible(true);
		formLayout.addFormItem(authSMSLimit, msg.getMessage("SMSCredentialDefinitionEditor.smsLimit"));
		binder.forField(authSMSLimit).asRequired().bind("authnSMSLimit");
		formLayout.add(new Hr());

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
		recoverySettings.addEditorToLayout(formLayout);
		formLayout.add(new Hr());
		
		MobileNumberConfirmationConfiguration confirmationConfig = null;
		if (helper.getMobileNumberConfirmationConfiguration().isPresent())
		{	confirmationConfig = helper.getMobileNumberConfirmationConfiguration()
					.get();
		}
		confirmationConfigEditor = new MobileNumberConfirmationConfigurationEditor(
				confirmationConfig, msg, msgTplMan,
				"SMSCredentialDefinitionEditor.newMobile.",
				SMSCredential.DEFAULT_VALIDITY);

		formLayout.add(confirmationConfigEditor);
		
		return formLayout;
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
