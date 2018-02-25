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
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.identities.IdentityFormatter;
import pl.edu.icm.unity.webui.confirmations.EmailConfirmationConfigurationEditor;
import pl.edu.icm.unity.webui.confirmations.EmailConfirmationConfigurationViewer;

/**
 * VerifiableEmail attribute handler for the web
 * 
 * @author P. Piernik
 */
public class VerifiableEmailAttributeHandler implements WebAttributeHandler
{
	private UnityMessageSource msg;
	private IdentityFormatter formatter;
	private VerifiableEmailAttributeSyntax syntax;
	private EmailConfirmationManager emailConfirmationMan;

	public VerifiableEmailAttributeHandler(UnityMessageSource msg, IdentityFormatter formatter, 
			AttributeValueSyntax<?> syntax, EmailConfirmationManager emailConfirmationMan)
	{
		this.msg = msg;
		this.formatter = formatter;
		this.syntax = (VerifiableEmailAttributeSyntax) syntax;
		this.emailConfirmationMan = emailConfirmationMan;
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
		ret.addComponent(info);
		ret.addComponent(new EmailConfirmationConfigurationViewer(msg, syntax.getEmailConfirmationConfiguration()));
		return ret;
	}


	private static class VerifiableEmailSyntaxEditor implements AttributeSyntaxEditor<VerifiableEmail>
	{

		
		private VerifiableEmailAttributeSyntax initial;
		private UnityMessageSource msg;
		private MessageTemplateManagement msgTemplateMan;
		private EmailConfirmationConfigurationEditor editor;
		
			
		public VerifiableEmailSyntaxEditor(VerifiableEmailAttributeSyntax initial,
				UnityMessageSource msg, MessageTemplateManagement msgTemplateMan)
		{
			this.initial = initial;
			this.msg = msg;
			this.msgTemplateMan = msgTemplateMan;
		}

		@Override
		public Component getEditor()
		{

			EmailConfirmationConfiguration confirmationConfig = null;
			if (initial != null)
				confirmationConfig = initial.getEmailConfirmationConfiguration();

			editor = new EmailConfirmationConfigurationEditor(confirmationConfig, msg,
					msgTemplateMan);
			return editor;

		}

		@Override
		public AttributeValueSyntax<VerifiableEmail> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			VerifiableEmailAttributeSyntax syntax = new VerifiableEmailAttributeSyntax();
			syntax.setEmailConfirmationConfiguration(editor.getCurrentValue());
			return syntax;
		}
	

	}

	private class VerifiableEmailValueEditor implements AttributeValueEditor
	{
		private VerifiableEmail value;
		private String label;
		private boolean required;
		private boolean adminMode;
		private ConfirmationInfo confirmationInfo;
		private TextFieldWithVerifyButton editor;

		public VerifiableEmailValueEditor(String valueRaw, String label)
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
			
			editor = new TextFieldWithVerifyButton(adminMode, required, msg.getMessage(
					"VerifiableEmailAttributeHandler.resendConfirmation"),
					Images.messageSend.getResource(),
					msg.getMessage("VerifiableEmailAttributeHandler.confirmedCheckbox"),
					false);
			if (label != null)
				editor.setTextFieldId("EmailValueEditor." + label);

			if (value != null)
				editor.setValue(value.getValue());

			if (value != null)
				editor.setAdminCheckBoxValue(value.isConfirmed());

			if (confirmationInfo.isConfirmed() || owner == null || value == null)
				editor.removeVerifyButton();

			editor.addVerifyButtonClickListener(e -> {

				if (value != null)
				{
					ConfirmDialog confirm = new ConfirmDialog(msg, msg
							.getMessage("VerifiableEmailAttributeHandler.confirmResendConfirmation"),
							() -> sendConfirmation(owner, group,
									attrName,
									value.getValue()));
					confirm.show();
				}

			});

			editor.addTextFieldValueChangeListener(e -> {

				if (value == null)
				{
					editor.setVerifyButtonVisiable(false);
					return;
				}

				if (e.getValue().equals(value.getValue()))
				{
					editor.setVerifyButtonVisiable(true);
				} else
				{
					editor.setVerifyButtonVisiable(false);
				}
			});

			return new ComponentsContainer(editor);

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
				NotificationPopup.showError(msg, msg.getMessage(
						"VerifiableEmailAttributeHandler.confirmationSendError",
						attrName), e1);
			
			}
		}
		
		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			if (!required && editor.getValue().isEmpty())
				return null;

			try
			{
				VerifiableEmail email = new VerifiableEmail(editor.getValue());
				if (adminMode)
					email.setConfirmationInfo(new ConfirmationInfo(
							editor.getAdminCheckBoxValue()));
				syntax.validate(email);
				editor.setComponentError(null);
				return syntax.convertToString(email);
			} catch (IllegalAttributeValueException e)
			{
				e.printStackTrace();
				editor.setComponentError(new UserError(e.getMessage()));
				throw e;
			} catch (Exception e)
			{
				e.printStackTrace();
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
	public static class VerifiableEmailAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;
		private IdentityFormatter formatter;
		private MessageTemplateManagement msgTemplateMan;
		private EmailConfirmationManager emailConfirmationMan;

		@Autowired
		public VerifiableEmailAttributeHandlerFactory(UnityMessageSource msg,
				IdentityFormatter formatter,
				MessageTemplateManagement msgTemplateMan,
				EmailConfirmationManager emailConfirmationMan)
		{
			this.msg = msg;
			this.formatter = formatter;
			this.msgTemplateMan = msgTemplateMan;
			this.emailConfirmationMan = emailConfirmationMan;
		}
		
		@Override
		public String getSupportedSyntaxId()
		{
			return VerifiableEmailAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new VerifiableEmailAttributeHandler(msg, formatter, syntax, emailConfirmationMan);
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
