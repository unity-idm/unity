/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.elements.TextFieldWithVerifyButton;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.confirmations.MobileNumberConfirmationConfigurationEditor;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.*;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ConfirmationInfoFormatter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import io.imunity.vaadin.endpoint.common.plugins.credentials.sms.MobileNumberConfirmationDialog;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.verifiable.VerifiableMobileNumber;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;

import java.util.Optional;

class VerifiableMobileNumberAttributeHandler implements WebAttributeHandler
{	
	private final MessageSource msg;
	private final ConfirmationInfoFormatter formatter;
	private final VerifiableMobileNumberAttributeSyntax syntax;
	private final MobileNumberConfirmationManager  mobileConfirmationMan;
	private final NotificationPresenter  notificationPresenter;

	VerifiableMobileNumberAttributeHandler(MessageSource msg,
			ConfirmationInfoFormatter formatter, AttributeValueSyntax<?> syntax,
			MobileNumberConfirmationManager mobileConfirmationMan,
	        NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.formatter = formatter;
		this.syntax = (VerifiableMobileNumberAttributeSyntax) syntax;
		this.mobileConfirmationMan = mobileConfirmationMan;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getValueAsString(String value)
	{
		VerifiableMobileNumber domainValue = syntax.convertFromString(value);
		return domainValue.getValue() + formatter
				.getConfirmationStatusString(domainValue.getConfirmationInfo());
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
		Span info = new Span(
				msg.getMessage("VerifiableMobileNumberAttributeHandler.info"));
		ret.add(info);
		Span msgTemplate = new Span();
		ret.add(msgTemplate);
		Span validityTime = new Span();
		ret.add(validityTime);
		Span codeLength = new Span();
		ret.add(codeLength);
		Optional<MobileNumberConfirmationConfiguration> config = syntax
				.getMobileNumberConfirmationConfiguration();

		msgTemplate.setText(msg.getMessage(
				"MobileNumberConfirmationConfiguration.confirmationMsgTemplate")
				+ " " + (config.isPresent() ? config.get().getMessageTemplate() : ""));

		validityTime.setText(msg.getMessage(
				"MobileNumberConfirmationConfiguration.validityTime") + " "
				+ (config.map(mobileNumberConfirmationConfiguration -> String.valueOf(mobileNumberConfirmationConfiguration.getValidityTime())).orElse("")));
		codeLength.setText(msg.getMessage(
				"MobileNumberConfirmationConfiguration.codeLength") + " "
				+ (config.map(mobileNumberConfirmationConfiguration -> String.valueOf(mobileNumberConfirmationConfiguration.getCodeLength())).orElse("")));
		return ret;
	}

	private static class VerifiableMobileNumberSyntaxEditor
			implements AttributeSyntaxEditor<VerifiableMobileNumber>
	{

		private final VerifiableMobileNumberAttributeSyntax initial;
		private final MessageSource msg;
		private final MessageTemplateManagement msgTemplateMan;
		private MobileNumberConfirmationConfigurationEditor editor;

		public VerifiableMobileNumberSyntaxEditor(
				VerifiableMobileNumberAttributeSyntax initial,
				MessageSource msg, MessageTemplateManagement msgTemplateMan)
		{
			this.initial = initial;
			this.msg = msg;
			this.msgTemplateMan = msgTemplateMan;
		}

		@Override
		public Optional<Component>  getEditor()
		{

			MobileNumberConfirmationConfiguration confirmationConfig = null;
			if (initial != null && initial.getMobileNumberConfirmationConfiguration().isPresent())
				confirmationConfig = initial
						.getMobileNumberConfirmationConfiguration().get();
			
			editor = new MobileNumberConfirmationConfigurationEditor(confirmationConfig,
					msg, msgTemplateMan);
			return Optional.of(editor);

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

		private final VerifiableMobileNumber value;
		private final String label;
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
					.getConfirmationMode() == ConfirmationEditMode.FORCE_CONFIRMED_IF_SYNC;
			confirmationInfo = value == null ? new ConfirmationInfo()
					: value.getConfirmationInfo();

			Optional<MobileNumberConfirmationConfiguration> confirmationConfig = mobileConfirmationMan
					.getConfirmationConfigurationForAttribute(
							context.getAttributeType().getName());
			editor = new TextFieldWithVerifyButton(
					context.getConfirmationMode() == ConfirmationEditMode.ADMIN,
					msg.getMessage("VerifiableMobileNumberAttributeHandler.verify"),
					VaadinIcon.MOBILE_RETRO.create(),
					msg.getMessage("VerifiableMobileNumberAttributeHandler.confirmedCheckbox"),
					context.isShowLabelInline());
			if (label != null)
				editor.setTextFieldId("MobileNumberValueEditor." + label);

			if (value != null) 
			{
				editor.setValue(value.getValue());
				editor.setAdminCheckBoxValue(value.isConfirmed());
			}
			
			if (confirmationConfig.isEmpty())
				editor.removeVerifyButton();
			
			if (context.getConfirmationMode() == ConfirmationEditMode.OFF)
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
						this::updateConfirmationStatusIconAndButtons,
						notificationPresenter);
				confirmationDialog.open();
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
				WebSession.getCurrent().getEventBus().fireEvent(new AttributeModyficationEvent());

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
				editor.setComponentError(message);
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
		VerifiableMobileNumber verifiableMobileNumber = VerifiableMobileNumber.fromJsonString(value);
		Component representation = AttributeHandlerHelper.getRepresentation(verifiableMobileNumber.getValue(), context);
		if(!verifiableMobileNumber.getConfirmationInfo().isConfirmed())
			return AttributeHandlerHelper.getRepresentationWithWarning(representation, msg);

		return representation;
	}

	@org.springframework.stereotype.Component
	static class VerifiableMobileNumberAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;
		private final ConfirmationInfoFormatter formatter;
		private final MessageTemplateManagement msgTemplateMan;
		private final MobileNumberConfirmationManager smsConfirmationMan;
		private final NotificationPresenter notificationPresenter;

		@Autowired
		VerifiableMobileNumberAttributeHandlerFactory(MessageSource msg,
		                                              ConfirmationInfoFormatter formatter,
		                                              MessageTemplateManagement msgTemplateMan,
		                                              MobileNumberConfirmationManager smsConfirmationMan,
		                                              NotificationPresenter notificationPresenter)
		{
			this.msg = msg;
			this.formatter = formatter;
			this.msgTemplateMan = msgTemplateMan;
			this.smsConfirmationMan = smsConfirmationMan;
			this.notificationPresenter = notificationPresenter;
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
					smsConfirmationMan, notificationPresenter);
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
