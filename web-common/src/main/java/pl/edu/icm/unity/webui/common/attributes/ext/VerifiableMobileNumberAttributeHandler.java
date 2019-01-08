/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
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
import pl.edu.icm.unity.types.basic.VerifiableMobileNumber;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.ReadOnlyField;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext.ConfirmationMode;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.binding.SingleStringFieldBinder;
import pl.edu.icm.unity.webui.common.binding.StringBindingValue;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;
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
	private ConfirmationInfoFormatter formatter;
	private VerifiableMobileNumberAttributeSyntax syntax;
	private MobileNumberConfirmationManager  mobileConfirmationMan;

	public VerifiableMobileNumberAttributeHandler(UnityMessageSource msg,
			ConfirmationInfoFormatter formatter, AttributeValueSyntax<?> syntax,
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
		Label codeLength = new Label();
		ret.addComponent(codeLength);
		Optional<MobileNumberConfirmationConfiguration> config = syntax
				.getMobileNumberConfirmationConfiguration();

		msgTemplate.setValue(msg.getMessage(
				"MobileNumberConfirmationConfiguration.confirmationMsgTemplate")
				+ " " + (config.isPresent() ? config.get().getMessageTemplate() : ""));

		validityTime.setValue(msg.getMessage(
				"MobileNumberConfirmationConfiguration.validityTime") + " "
				+ (config.isPresent()? String.valueOf(config.get().getValidityTime()) : ""));
		codeLength.setValue(msg.getMessage(
				"MobileNumberConfirmationConfiguration.codeLength") + " "
				+ (config.isPresent() ? String.valueOf(config.get().getCodeLength()) : ""));
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
			if (initial != null && initial.getMobileNumberConfirmationConfiguration().isPresent())
				confirmationConfig = initial
						.getMobileNumberConfirmationConfiguration().get();
			
			editor = new MobileNumberConfirmationConfigurationEditor(confirmationConfig,
					msg, msgTemplateMan);
			return editor;

		}

		@Override
		public AttributeValueSyntax<VerifiableMobileNumber> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			VerifiableMobileNumberAttributeSyntax syntax = new VerifiableMobileNumberAttributeSyntax();
			MobileNumberConfirmationConfiguration config;
			try
			{
				config = editor.getCurrentValue();
			} catch (FormValidationException e)
			{
				throw new IllegalAttributeTypeException("", e);
			}

			syntax.setMobileNumberConfirmationConfiguration(config);
			return syntax;
		}

	}

	private class VerifiableMobileNumberValueEditor implements AttributeValueEditor
	{

		private VerifiableMobileNumber value;
		private String label;
		private TextFieldWithVerifyButton editor;
		private ConfirmationInfo confirmationInfo;
		private boolean forceConfirmed = false;
		private boolean skipUpdate = false;
		private SingleStringFieldBinder binder;
		
		public VerifiableMobileNumberValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{	
			this.forceConfirmed = context
					.getConfirmationMode() == ConfirmationMode.FORCE_CONFIRMED;
			confirmationInfo = value == null ? new ConfirmationInfo()
					: value.getConfirmationInfo();

			Optional<MobileNumberConfirmationConfiguration> confirmationConfig = mobileConfirmationMan
					.getConfirmationConfigurationForAttribute(
							context.getAttributeType().getName());
			editor = new TextFieldWithVerifyButton(
					context.getConfirmationMode() == ConfirmationMode.ADMIN,
					msg.getMessage("VerifiableMobileNumberAttributeHandler.verify"),
					Images.mobile.getResource(),
					msg.getMessage("VerifiableMobileNumberAttributeHandler.confirmedCheckbox"),
					context.isShowLabelInline());
			if (label != null)
				editor.setTextFieldId("MobileNumberValueEditor." + label);

			if (value != null) 
			{
				editor.setValue(value.getValue());
				editor.setAdminCheckBoxValue(value.isConfirmed());
			}
			
			if (!confirmationConfig.isPresent())
				editor.removeVerifyButton();
			
			if (context.getConfirmationMode() == ConfirmationMode.OFF)
			{
				editor.removeConfirmationStatusIcon();
				editor.removeVerifyButton();
			}
			
			editor.addVerifyButtonClickListener(e -> {
				String value;
				try
				{
					value = getCurrentValue(false);
				} catch (IllegalAttributeValueException e1)
				{
					return; 
				}

				MobileNumberConfirmationDialog confirmationDialog = new MobileNumberConfirmationDialog(
						syntax.convertFromString(value).getValue(),
						confirmationInfo, msg, mobileConfirmationMan,
						confirmationConfig.get(),
						() -> updateConfirmationStatusIconAndButtons());
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
				updateConfirmationStatusIconAndButtons();
			});
			
			editor.addAdminConfirmCheckBoxValueChangeListener(e -> {
				if (!skipUpdate)
				{
					confirmationInfo = new ConfirmationInfo(e.getValue());
					updateConfirmationStatusIconAndButtons();
				}
			});
			
			updateConfirmationStatusIconAndButtons();
			
			if (context.isCustomWidth())
				editor.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			
			binder = new SingleStringFieldBinder(msg);
			binder.forField(editor, context.isRequired())
				.withValidator(this::validate)
				.bind("value");
			binder.setBean(new StringBindingValue(value == null ? "" : value.getValue()));
			
			return new ComponentsContainer(editor);
		}
		
		private void updateConfirmationStatusIconAndButtons()
		{
			editor.setConfirmationStatusIcon(formatter.getSimpleConfirmationStatusString(
					confirmationInfo), confirmationInfo.isConfirmed());
			editor.setVerifyButtonVisible(!confirmationInfo.isConfirmed() && !editor.getValue().isEmpty());
			skipUpdate = true;
			editor.setAdminCheckBoxValue(confirmationInfo.isConfirmed());	
			skipUpdate = false;
			if (confirmationInfo.isConfirmed())
				editor.setComponentError(null);
		}

		private ValidationResult validate(String value, ValueContext context)
		{
			if (value.isEmpty())
				return ValidationResult.ok(); //fall through
			try
			{
				VerifiableMobileNumber mobile = new VerifiableMobileNumber(value);
				mobile.setConfirmationInfo(confirmationInfo);
				syntax.validate(mobile);
				return ValidationResult.ok();
			} catch (Exception e)
			{
				return ValidationResult.error(e.getMessage());
			}
		}

		
		private String getCurrentValue(boolean forceConfirmation) throws IllegalAttributeValueException
		{
			binder.ensureValidityCatched(() -> new IllegalAttributeValueException(""));
			String value = binder.getBean().getValue().trim();
			if (value.isEmpty())
				return null;
			
			if (forceConfirmation && !confirmationInfo.isConfirmed())
			{
				String message = msg.getMessage("VerifiableMobileNumberAttributeHandler.confirmationRequired");
				editor.setComponentError(new UserError(message));
				throw new IllegalAttributeValueException("");
			}
			VerifiableMobileNumber mobile = new VerifiableMobileNumber(value);
			mobile.setConfirmationInfo(confirmationInfo);
			return syntax.convertToString(mobile);
		}
		
		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			return getCurrentValue(forceConfirmed);	
		}

		@Override
		public void setLabel(String label)
		{
			editor.setLabel(label);
		}
	}

	@Override
	public Component getRepresentation(String value, AttributeViewerContext context)
	{
		Component component = new ReadOnlyField(getValueAsString(value));
		if (context.isCustomWidth())
			component.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		return component;
	}

	@org.springframework.stereotype.Component
	public static class VerifiableMobileNumberAttributeHandlerFactory
			implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;
		private ConfirmationInfoFormatter formatter;
		private MessageTemplateManagement msgTemplateMan;
		private MobileNumberConfirmationManager smsConfirmationMan;

		@Autowired
		public VerifiableMobileNumberAttributeHandlerFactory(UnityMessageSource msg,
				ConfirmationInfoFormatter formatter,
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
