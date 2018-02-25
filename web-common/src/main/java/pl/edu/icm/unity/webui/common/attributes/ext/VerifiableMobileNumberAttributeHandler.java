/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.UserError;
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
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.confirmations.MobileNumberConfirmationConfigurationEditor;
import pl.edu.icm.unity.webui.confirmations.MobileNumberConfirmationDialog;

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
	private MobileNumberConfirmationManager  mobileConfirmationMan;

	public VerifiableMobileNumberAttributeHandler(UnityMessageSource msg,
			IdentityFormatter formatter, AttributeValueSyntax<?> syntax,
			MobileNumberConfirmationManager mobileConfirmationMan)
	{
		this.msg = msg;
		this.formatter = formatter;
		this.syntax = (VerifiableMobileNumberAttributeSyntax) syntax;
		this.mobileConfirmationMan = mobileConfirmationMan;
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
		return new VerifiableMobileNumberValueEditor(initialValue, label);
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
		private TextFieldWithVerifyButton editor;
		private ConfirmationInfo confirmationInfo;
		private boolean required;
		private boolean adminMode;
		
		
		public VerifiableMobileNumberValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(boolean required, boolean adminMode,
				String attrName, EntityParam owner, String group)
		{	
			this.required = required;
			this.adminMode = adminMode;
			
			confirmationInfo = value == null ? new ConfirmationInfo()
					: value.getConfirmationInfo();

			MobileNumberConfirmationConfiguration confirmationConfig = mobileConfirmationMan.getConfirmationConfigurationForAttribute(
					attrName);
			
			editor = new TextFieldWithVerifyButton(adminMode, required, msg.getMessage(
					"VerifiableMobileNumberAttributeHandler.verify"),
					Images.mobile.getResource(),
					msg.getMessage("VerifiableMobileNumberAttributeHandler.confirmedCheckbox"),
					!adminMode);
			if (label != null)
				editor.setTextFieldId("MobileNumberValueEditor." + label);

			if (value != null)
				editor.setValue(value.getValue());

			if (value != null)
				editor.setAdminCheckBoxValue(value.isConfirmed());
			
			if (confirmationConfig == null)
				editor.removeVerifyButton();
			
			editor.addVerifyButtonClickListener(e -> {
				String value;
				try
				{
					value = getCurrentValue();
				} catch (IllegalAttributeValueException e1)
				{
					return; 
				}

				MobileNumberConfirmationDialog confirmationDialog = new MobileNumberConfirmationDialog(
						syntax.convertFromString(value).getValue(),
						confirmationInfo, msg, mobileConfirmationMan,
						confirmationConfig,
						new MobileNumberConfirmationDialog.Callback()
						{
							@Override
							public void onConfirm()
							{
								updateConfirmationLabelAndButtons();
							}
						});
				confirmationDialog.show();
			});

			editor.addTextFieldValueChangeListener(e -> {
				
				if (value != null && e.getValue().equals(value.getValue()))
				{
					confirmationInfo = value.getConfirmationInfo();
				} else
				{
					confirmationInfo = new ConfirmationInfo();
				}
				updateConfirmationLabelAndButtons();
			});
			
			editor.addAdminConfirmCheckBoxValueChangeListener(e -> {
				editor.setVerifyButtonVisiable(!e.getValue() && !editor.getValue().isEmpty());
			});
			
			updateConfirmationLabelAndButtons();
			return new ComponentsContainer(editor);
		}
		
		private void updateConfirmationLabelAndButtons()
		{
			editor.setInfoLabelValue(formatter.getConfirmationStatusString(
					confirmationInfo));
			editor.setVerifyButtonVisiable(!confirmationInfo.isConfirmed() && !editor.getValue().isEmpty());
			editor.setAdminCheckBoxValue(confirmationInfo.isConfirmed());	
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{

			if (!required && editor.getValue().isEmpty())
				return null;
			
			try
			{
				VerifiableMobileNumber mobile = new VerifiableMobileNumber(editor.getValue());
				if (adminMode)
				{
					mobile.setConfirmationInfo(
							new ConfirmationInfo(editor.getAdminCheckBoxValue()));
				} else
				{
					mobile.setConfirmationInfo(confirmationInfo);
				}
				syntax.validate(mobile);
				editor.setComponentError(null);
				return syntax.convertToString(mobile);
			} catch (IllegalAttributeValueException e)
			{
				editor.setComponentError(new UserError(e.getMessage()));
				throw e;
			} catch (Exception e)
			{
				editor.setComponentError(new UserError(e.getMessage()));
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}		
		}

		@Override
		public void setLabel(String label)
		{
			editor.setCaption(label);

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
