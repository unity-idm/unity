/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
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

	public VerifiableEmailAttributeHandler(UnityMessageSource msg, IdentityFormatter formatter, 
			AttributeValueSyntax<?> syntax)
	{
		this.msg = msg;
		this.formatter = formatter;
		this.syntax = (VerifiableEmailAttributeSyntax) syntax;
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
		ret.addComponent(new EmailConfirmationConfigurationViewer(msg,
				syntax.getEmailConfirmationConfiguration().get()));
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
			syntax.setEmailConfirmationConfiguration(editor.getCurrentValue());
			return syntax;
		}
	

	}

	private class VerifiableEmailValueEditor implements AttributeValueEditor
	{
		private VerifiableEmail value;
		private String label;
		private AbstractTextField field;
		private CheckBox confirmed;
		private boolean required;
		private boolean adminMode;

		public VerifiableEmailValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(boolean required, boolean adminMode)
		{
			this.required = required;
			this.adminMode = adminMode;
			field = new TextField();
			field.setCaption(label);
			if (label != null)
				field.setId("EmailValueEditor."+label);
			if (value != null)
				field.setValue(value.getValue());
			field.setRequiredIndicatorVisible(required);
			ComponentsContainer ret = new ComponentsContainer(field);
			if (adminMode)
			{
				confirmed = new CheckBox(msg.getMessage(
						"VerifiableEmailAttributeHandler.confirmedCheckbox"));
				ret.add(confirmed);
				if (value != null)
					confirmed.setValue(value.isConfirmed());
			} else
			{
				if (value != null && value.getConfirmationInfo() != null && 
						value.getConfirmationInfo().isConfirmed())
					ret.add(new Label(formatter.getConfirmationStatusString(
							value.getConfirmationInfo())));
			}
			return ret;
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			if (!required && field.getValue().isEmpty())
				return null;
			
			try
			{
				VerifiableEmail email = new VerifiableEmail(field.getValue());
				if (adminMode)
					email.setConfirmationInfo(new ConfirmationInfo(confirmed.getValue()));
				syntax.validate(email);
				field.setComponentError(null);
				return syntax.convertToString(email);
			} catch (IllegalAttributeValueException e)
			{
				field.setComponentError(new UserError(e.getMessage()));
				throw e;
			} catch (Exception e)
			{
				field.setComponentError(new UserError(e.getMessage()));
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}
		}

		@Override
		public void setLabel(String label)
		{
			field.setCaption(label);

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

		@Autowired
		public VerifiableEmailAttributeHandlerFactory(UnityMessageSource msg, IdentityFormatter formatter, MessageTemplateManagement msgTemplateMan)
		{
			this.msg = msg;
			this.formatter = formatter;
			this.msgTemplateMan = msgTemplateMan;
		}
		

		@Override
		public String getSupportedSyntaxId()
		{
			return VerifiableEmailAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new VerifiableEmailAttributeHandler(msg, formatter, syntax);
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
