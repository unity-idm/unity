/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.components.StringBindingValue;
import io.imunity.vaadin23.shared.endpoint.components.TextFieldWithVerifyButton;
import io.imunity.vaadin23.shared.endpoint.forms.registration.SingleStringFieldBinder;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.*;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.ConfirmationEditMode;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

import java.util.Optional;

public class VerifiableEmailAttributeHandler implements WebAttributeHandler
{
	private MessageSource msg;
	private ConfirmationInfoFormatter formatter;
	private VerifiableEmailAttributeSyntax syntax;
	private EmailConfirmationManager emailConfirmationMan;
	private NotificationPresenter notificationPresenter;

	public VerifiableEmailAttributeHandler(MessageSource msg, ConfirmationInfoFormatter formatter, 
			AttributeValueSyntax<?> syntax, EmailConfirmationManager emailConfirmationMan, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.formatter = formatter;
		this.syntax = (VerifiableEmailAttributeSyntax) syntax;
		this.emailConfirmationMan = emailConfirmationMan;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getValueAsString(String value)
	{
		VerifiableEmail domainValue = syntax.convertFromString(value);
		StringBuilder rep = new StringBuilder(domainValue.getValue());
		rep.append(formatter.getConfirmationStatusString(domainValue.getConfirmationInfo()));
		return rep.toString();
	}

	
	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new VerifiableEmailValueEditor(initialValue, label);
	}

	@Override
	public Component getSyntaxViewer()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(false);
		ret.setMargin(false);
		Label info = new Label(msg.getMessage("VerifiableEmailAttributeHandler.info"));
		ret.add(info);
		if (syntax.getEmailConfirmationConfiguration().isPresent())
			ret.add(new EmailConfirmationConfigurationViewer(msg,
				syntax.getEmailConfirmationConfiguration().get()));
		return ret;
	}


	private static class VerifiableEmailSyntaxEditor implements AttributeSyntaxEditor<VerifiableEmail>
	{
		private VerifiableEmailAttributeSyntax initial;
		private MessageSource msg;
		private MessageTemplateManagement msgTemplateMan;
		private EmailConfirmationConfigurationEditor editor;
		
			
		public VerifiableEmailSyntaxEditor(VerifiableEmailAttributeSyntax initial,
				MessageSource msg, MessageTemplateManagement msgTemplateMan)
		{
			this.initial = initial;
			this.msg = msg;
			this.msgTemplateMan = msgTemplateMan;
		}

		@Override
		public Component getEditor()
		{
			EmailConfirmationConfiguration confirmationConfig = null;
			if (initial != null && initial.getEmailConfirmationConfiguration().isPresent())
				confirmationConfig = initial.getEmailConfirmationConfiguration().get();

			editor = new EmailConfirmationConfigurationEditor(confirmationConfig, msg,
					msgTemplateMan);
			return editor;

		}

		@Override
		public AttributeValueSyntax<VerifiableEmail> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			VerifiableEmailAttributeSyntax syntax = new VerifiableEmailAttributeSyntax();
			EmailConfirmationConfiguration config;
			try
			{
				config = editor.getCurrentValue();
			} catch (FormValidationException e)
			{
				throw new IllegalAttributeTypeException("", e);
			}
			syntax.setEmailConfirmationConfiguration(config);
			return syntax;
		}
	

	}

	private class VerifiableEmailValueEditor implements AttributeValueEditor
	{
		private VerifiableEmail value;
		private String label;
		private ConfirmationInfo confirmationInfo;
		private TextFieldWithVerifyButton editor;
		private boolean skipUpdate = false;
		private SingleStringFieldBinder binder;
		
		public VerifiableEmailValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			confirmationInfo = value == null ? new ConfirmationInfo()
					: value.getConfirmationInfo();

			Optional<EmailConfirmationConfiguration> confirmationConfig = emailConfirmationMan
					.getConfirmationConfigurationForAttribute(
							context.getAttributeType().getName());
			editor = new TextFieldWithVerifyButton(
					context.getConfirmationMode() == ConfirmationEditMode.ADMIN,
					msg.getMessage("VerifiableEmailAttributeHandler.resendConfirmation"),
					VaadinIcon.ENVELOPE_O.create(),
					msg.getMessage("VerifiableEmailAttributeHandler.confirmedCheckbox"),
					context.isShowLabelInline());
			if (label != null)
				editor.setTextFieldId("EmailValueEditor." + label);

			if (value != null)
			{
				editor.setValue(value.getValue());
				editor.setAdminCheckBoxValue(value.isConfirmed());
			} else
			{
				editor.removeConfirmationStatusIcon();
			}		
			
			if (confirmationInfo.isConfirmed() || context.getAttributeOwner() == null || value == null
					|| !confirmationConfig.isPresent())
				editor.removeVerifyButton();

			if (!context.getConfirmationMode().isShowVerifyButton())
				editor.removeVerifyButton();
			if (!context.getConfirmationMode().isShowConfirmationStatus())
				editor.removeConfirmationStatusIcon();
			
			editor.addVerifyButtonClickListener(e -> onVerifyButtonClick(context));

			editor.addTextFieldValueChangeListener(e -> 
			{
				if (value != null && e.getValue().equals(value.getValue()))
				{
					confirmationInfo = value.getConfirmationInfo();
				} else
				{
					confirmationInfo = new ConfirmationInfo();
				}
				updateConfirmationStatusIcon();
			});

			editor.addAdminConfirmCheckBoxValueChangeListener(e -> 
			{	
				if (!skipUpdate)
				{
					confirmationInfo = new ConfirmationInfo(e.getValue());
					updateConfirmationStatusIcon();
				}
			});
			
			updateConfirmationStatusIcon();
			
			if (context.isCustomWidth())
				editor.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			
			binder = new SingleStringFieldBinder(msg);
			binder.forField(editor, context.isRequired())
				.withValidator(this::validate)
				.bind("value");
			binder.setBean(new StringBindingValue(value == null ? "" : value.getValue()));
			
			return new ComponentsContainer(editor);

		}

		private void onVerifyButtonClick(AttributeEditContext context)
		{
			if (value == null)
				return;
			ConfirmDialog confirm = new ConfirmDialog(
					msg.getMessage("ConfirmDialog.confirm"),
					msg.getMessage("VerifiableEmailAttributeHandler.confirmResendConfirmation"),
					"OK",
					event -> {
						sendConfirmation(context.getAttributeOwner(), 
								context.getAttributeGroup(),
								context.getAttributeType().getName(),
								value.getValue());
						confirmationInfo.setSentRequestAmount(confirmationInfo.getSentRequestAmount() + 1);
						updateConfirmationStatusIcon();
					});
			confirm.open();
		}
		
		private ValidationResult validate(String value, ValueContext context)
		{
			if (value.isEmpty())
				return ValidationResult.ok(); //fall back
			try
			{
				VerifiableEmail email = new VerifiableEmail(value);
				email.setConfirmationInfo(confirmationInfo);
				syntax.validate(email);
				return ValidationResult.ok();
			} catch (Exception e)
			{
				return ValidationResult.error(e.getMessage());
			}
		}
		
		private void updateConfirmationStatusIcon()
		{
			editor.setConfirmationStatusIcon(formatter.getSimpleConfirmationStatusString(
					confirmationInfo), confirmationInfo.isConfirmed());
			editor.setVerifyButtonVisible(!confirmationInfo.isConfirmed()
					&& !editor.getValue().isEmpty()
					&& value != null && editor.getValue().equals(value.getValue()));
			skipUpdate = true;
			editor.setAdminCheckBoxValue(confirmationInfo.isConfirmed());	
			skipUpdate = false;
		}
		
		
		private void sendConfirmation(EntityParam owner,String group, String attrName, String attrValue)
		{
			try
			{
				emailConfirmationMan.sendVerification(owner,
						VerifiableEmailAttribute.of(attrName, group,
								attrValue));
				
			} catch (EngineException e1)
			{
				notificationPresenter.showError(msg.getMessage(
						"VerifiableEmailAttributeHandler.confirmationSendError",
						attrName), e1.getMessage());
			
			}
		}
		
		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			binder.ensureValidityCatched(() -> new IllegalAttributeValueException(""));
			String emailVal = binder.getBean().getValue().trim();
			if (emailVal.isEmpty())
				return null;
			VerifiableEmail email = new VerifiableEmail(emailVal);
			email.setConfirmationInfo(confirmationInfo);
			return syntax.convertToString(email);
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
		return AttributeHandlerHelper.getRepresentation(getValueAsString(value), context);
	}
	
	
	@org.springframework.stereotype.Component
	public static class VerifiableEmailAttributeHandlerFactoryV23 implements WebAttributeHandlerFactory
	{
		private MessageSource msg;
		private ConfirmationInfoFormatter formatter;
		private MessageTemplateManagement msgTemplateMan;
		private EmailConfirmationManager emailConfirmationMan;
		private NotificationPresenter notificationPresenter;


		@Autowired
		public VerifiableEmailAttributeHandlerFactoryV23(MessageSource msg,
		                                                 ConfirmationInfoFormatter formatter,
		                                                 MessageTemplateManagement msgTemplateMan,
		                                                 EmailConfirmationManager emailConfirmationMan,
		                                                 NotificationPresenter notificationPresenter)
		{
			this.msg = msg;
			this.formatter = formatter;
			this.msgTemplateMan = msgTemplateMan;
			this.emailConfirmationMan = emailConfirmationMan;
			this.notificationPresenter = notificationPresenter;
		}
		
		@Override
		public String getSupportedSyntaxId()
		{
			return VerifiableEmailAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new VerifiableEmailAttributeHandler(msg, formatter, syntax, emailConfirmationMan, notificationPresenter);
		}
		
		@Override
		public AttributeSyntaxEditor<VerifiableEmail> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new VerifiableEmailSyntaxEditor(
					(VerifiableEmailAttributeSyntax) initialValue, msg,
					msgTemplateMan);
		}
	}
}