/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

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
		StringBuilder rep = new StringBuilder();
		rep.append(value.getValue());
		ConfirmationInfo cdata = value.getConfirmationInfo();
		rep.append(" [");
		if (cdata != null)
		{
			rep.append(msg.getMessage("VerifiableEmailAttributeHandler.confirmed",
					cdata.isConfirmed()));
			if (cdata.isConfirmed())
			{
				if (cdata.getConfirmationDate() != 0)
				{
					rep.append("; " + msg.getMessage("VerifiableEmailAttributeHandler.confirmationDate",
									new Date(cdata.getConfirmationDate())));
				}
			} else
			{
				rep.append("; "+ msg.getMessage("VerifiableEmailAttributeHandler.sentRequests",
								cdata.getSentRequestAmount()));
			}
		}
		rep.append("]");
		return rep.toString();
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
		VerticalLayout ret = new VerticalLayout();
		Label info = new Label(msg.getMessage("VerifiableEmailAttributeHandler.info"));
		ret.addComponent(info);
		return ret;
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
			return new VerticalLayout();
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
				email.setValue(field.getValue());
				syntax.validate(email);
				field.setComponentError(null);
				if (value != null && field.getValue().equals(value.getValue()))
					email.setConfirmationInfo(value.getConfirmationInfo());
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

	@Override
	public Component getRepresentation(
			VerifiableEmail value,
			AttributeValueSyntax<VerifiableEmail> syntax,
			pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler.RepresentationSize size)
	{
		return new Label(getValueAsString(value, syntax, 80));
	}
}
