/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.confirmations.MobileNumberConfirmationConfigurationEditor;

/**
 * Verifiable mobile number attribute handler for the web
 * 
 * @author P. Piernik
 */
public class VerifiableMobileNumberAttributeHandler implements WebAttributeHandler
{	
	private UnityMessageSource msg;
	private IdentityFormatter formatter;
	private VerifiableMobileNumberAttributeSyntax syntax;
	private MobileNumberConfirmationManager smsConfirmationMan;

	public VerifiableMobileNumberAttributeHandler(UnityMessageSource msg,
			IdentityFormatter formatter, AttributeValueSyntax<?> syntax,
			MobileNumberConfirmationManager smsConfirmationMan)
	{
		this.msg = msg;
		this.formatter = formatter;
		this.syntax = (VerifiableMobileNumberAttributeSyntax) syntax;
		this.smsConfirmationMan = smsConfirmationMan;
	}

	@Override
	public String getValueAsString(String value)
	{
		VerifiableMobileNumber domainValue = syntax.convertFromString(value);
		StringBuilder rep = new StringBuilder(domainValue.getValue());
		rep.append(formatter
				.getConfirmationStatusString(domainValue.getConfirmationInfo()));
		return rep.toString();
	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new VerifiableMobileNumberValueEditor(initialValue, label,
				smsConfirmationMan);
	}

	@Override
	public Component getSyntaxViewer()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(false);
		ret.setMargin(false);
		Label info = new Label(
				msg.getMessage("VerifiableMobileNumberAttributeHandler.info"));
		ret.addComponent(info);
		Label msgTemplate = new Label();
		ret.addComponent(msgTemplate);
		Label validityTime = new Label();
		ret.addComponent(validityTime);
		Label codeLenght = new Label();
		ret.addComponent(codeLenght);
		MobileNumberConfirmationConfiguration config = syntax
				.getMobileNumberConfirmationConfiguration();

		msgTemplate.setValue(msg.getMessage(
				"MobileNumberConfirmationConfiguration.confirmationMsgTemplate")
				+ " " + (config != null ? config.getMessageTemplate() : ""));

		validityTime.setValue(msg.getMessage(
				"MobileNumberConfirmationConfiguration.validityTime") + " "
				+ (config != null ? String.valueOf(config.getValidityTime()) : ""));
		codeLenght.setValue(msg.getMessage(
				"MobileNumberConfirmationConfiguration.codeLenght") + " "
				+ (config != null ? String.valueOf(config.getCodeLenght()) : ""));
		return ret;
	}

	private static class VerifiableMobileNumberSyntaxEditor
			implements AttributeSyntaxEditor<VerifiableMobileNumber>
	{

		private VerifiableMobileNumberAttributeSyntax initial;
		private UnityMessageSource msg;
		private MessageTemplateManagement msgTemplateMan;
		private MobileNumberConfirmationConfigurationEditor editor;

		public VerifiableMobileNumberSyntaxEditor(
				VerifiableMobileNumberAttributeSyntax initial,
				UnityMessageSource msg, MessageTemplateManagement msgTemplateMan)
		{
			this.initial = initial;
			this.msg = msg;
			this.msgTemplateMan = msgTemplateMan;
		}

		@Override
		public Component getEditor()
		{

			MobileNumberConfirmationConfiguration confirmationConfig = null;
			if (initial != null)
				confirmationConfig = initial
						.getMobileNumberConfirmationConfiguration();

			editor = new MobileNumberConfirmationConfigurationEditor(confirmationConfig,
					msg, msgTemplateMan);
			return editor;

		}

		@Override
		public AttributeValueSyntax<VerifiableMobileNumber> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			VerifiableMobileNumberAttributeSyntax syntax = new VerifiableMobileNumberAttributeSyntax();
			syntax.setMobileNumberConfirmationConfiguration(editor.getCurrentValue());
			return syntax;
		}

	}

	private class VerifiableMobileNumberValueEditor implements AttributeValueEditor
	{

		private VerifiableMobileNumber value;
		private String label;
		private MobileNumberConfirmationManager smsConfirmationMan;

		private MobileNumberFieldWithVerifyButton mobileField;

		public VerifiableMobileNumberValueEditor(String valueRaw, String label,
				MobileNumberConfirmationManager smsConfirmationMan)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
			this.smsConfirmationMan = smsConfirmationMan;
		}

		@Override
		public ComponentsContainer getEditor(boolean required, boolean adminMode,
				String attrName)
		{

			ComponentsContainer ret = new ComponentsContainer();
			mobileField = new MobileNumberFieldWithVerifyButton(value, label,
					smsConfirmationMan, syntax, msg, formatter, required,
					adminMode,
					smsConfirmationMan.getConfirmationConfigurationForAttribute(
							attrName));
			ret.add(mobileField);
			return ret;

		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{

			return mobileField.getVerifiableMobileValue();
		}

		@Override
		public void setLabel(String label)
		{
			mobileField.setCaption(label);

		}
	}

	@Override
	public Component getRepresentation(String value)
	{
		return new Label(getValueAsString(value));
	}

	@org.springframework.stereotype.Component
	public static class VerifiableMobileNumberAttributeHandlerFactory
			implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		private IdentityFormatter formatter;

		private MessageTemplateManagement msgTemplateMan;

		private MobileNumberConfirmationManager smsConfirmationMan;

		@Autowired
		public VerifiableMobileNumberAttributeHandlerFactory(UnityMessageSource msg,
				IdentityFormatter formatter,
				MessageTemplateManagement msgTemplateMan,
				MobileNumberConfirmationManager smsConfirmationMan,
				AttributeTypeSupport attributeTypeSupport)
		{
			this.msg = msg;
			this.formatter = formatter;
			this.msgTemplateMan = msgTemplateMan;
			this.smsConfirmationMan = smsConfirmationMan;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return VerifiableMobileNumberAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new VerifiableMobileNumberAttributeHandler(msg, formatter, syntax,
					smsConfirmationMan);
		}

		@Override
		public AttributeSyntaxEditor<VerifiableMobileNumber> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new VerifiableMobileNumberSyntaxEditor(
					(VerifiableMobileNumberAttributeSyntax) initialValue, msg,
					msgTemplateMan);
		}
	}
}
