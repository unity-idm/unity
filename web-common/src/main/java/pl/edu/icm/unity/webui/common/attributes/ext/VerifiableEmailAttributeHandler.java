/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

/**
 * VerifiableEmail attribute handler for the web
 * 
 * @author P. Piernik
 */
@org.springframework.stereotype.Component
public class VerifiableEmailAttributeHandler implements WebAttributeHandler<VerifiableEmail>,
		WebAttributeHandlerFactory
{

	private UnityMessageSource msg;

	@Autowired
	public VerifiableEmailAttributeHandler(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return VerifiableEmailAttributeSyntax.ID;
	}

	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new VerifiableEmailAttributeHandler(msg);
	}

	@Override
	public String getValueAsString(VerifiableEmail value,
			AttributeValueSyntax<VerifiableEmail> syntax, int limited)
	{
		return new String(syntax.serialize(value));
	}

	@Override
	public Resource getValueAsImage(VerifiableEmail value,
			AttributeValueSyntax<VerifiableEmail> syntax, int maxWidth, int maxHeight)
	{
		return null;
	}

	@Override
	public Component getRepresentation(VerifiableEmail value,
			AttributeValueSyntax<VerifiableEmail> syntax)
	{
		FormLayout main = new FormLayout();
		Label val = new Label(value.getValue());
		Label ver = new Label(String.valueOf(value.isVerified()));
		ver.setCaption(msg.getMessage("VerifiableEmailAttributeHandler.verified"));
		Label date = new Label();
		date.setCaption(msg.getMessage("VerifiableEmailAttributeHandler.verificationDate"));
		if (value.isVerified() && value.getVerificationDate() != 0)
		{
			Date dt = new Date(value.getVerificationDate());
			date.setValue(new SimpleDateFormat(Constants.AMPM_DATE_FORMAT).format(dt));
			main.addComponents(val, ver, date);
		} else
		{
			main.addComponents(val, ver);
		}

		return main;
	}

	@Override
	public AttributeValueEditor<VerifiableEmail> getEditorComponent(
			VerifiableEmail initialValue, String label,
			AttributeValueSyntax<VerifiableEmail> syntaxDesc)
	{
		return new VerifiableEmailValueEditor(initialValue, label, syntaxDesc);
	}

	@Override
	public Component getSyntaxViewer(AttributeValueSyntax<VerifiableEmail> syntax)
	{
		return new FormLayout();
	}

	@Override
	public AttributeSyntaxEditor<VerifiableEmail> getSyntaxEditorComponent(
			AttributeValueSyntax<VerifiableEmail> initialValue)
	{
		return new VerifiableEmailSyntaxEditor();
	}

	private class VerifiableEmailSyntaxEditor implements AttributeSyntaxEditor<VerifiableEmail>
	{

		@Override
		public Component getEditor()
		{
			return new FormLayout();
		}

		@Override
		public AttributeValueSyntax<VerifiableEmail> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			return new VerifiableEmailAttributeSyntax();
		}

	}

	private class VerifiableEmailValueEditor implements AttributeValueEditor<VerifiableEmail>
	{
		private VerifiableEmail value;
		private String label;
		private AbstractTextField field;
		private AttributeValueSyntax<VerifiableEmail> syntax;
		private boolean required;

		public VerifiableEmailValueEditor(VerifiableEmail value, String label,
				AttributeValueSyntax<VerifiableEmail> syntax)
		{
			this.value = value;
			this.label = label;
			this.syntax = syntax;
		}

		@Override
		public ComponentsContainer getEditor(boolean required)
		{
			this.required = required;
			field = new TextField();
			field.setCaption(label);
			if (value != null)
				field.setValue(value.getValue());
			return new ComponentsContainer(field);

		}

		@Override
		public VerifiableEmail getCurrentValue() throws IllegalAttributeValueException
		{
			if (!required && field.getValue().isEmpty())
				return null;
			try
			{
				VerifiableEmail email = new VerifiableEmail();
				email.setValue(field.getValue());;
				syntax.validate(email);
				field.setComponentError(null);
				return email;
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
}
