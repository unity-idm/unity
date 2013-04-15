/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

/**
 * Enum attribute handler for the web
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class EnumAttributeHandler implements WebAttributeHandler<String>, WebAttributeHandlerFactory
{
	@Override
	public String getSupportedSyntaxId()
	{
		return EnumAttributeSyntax.ID;
	}
	
	@Override
	public Component getRepresentation(String value, AttributeValueSyntax<String> syntax)
	{
		return new Label(value.toString(), ContentMode.PREFORMATTED);
	}
	
	@Override
	public AttributeValueEditor<String> getEditorComponent(String initialValue, 
			AttributeValueSyntax<String> syntax)
	{
		return new EnumValueEditor(initialValue, (EnumAttributeSyntax) syntax);
	}
	
	private class EnumValueEditor implements AttributeValueEditor<String>
	{
		private String value;
		private EnumAttributeSyntax syntax;
		private ComboBox field;
		
		public EnumValueEditor(String value, EnumAttributeSyntax syntax)
		{
			this.value = value;
			this.syntax = syntax;
		}

		@Override
		public Component getEditor()
		{
			field = new ComboBox();
			field.setNullSelectionAllowed(false);
			for (String allowed: syntax.getAllowed())
				field.addItem(allowed);
			if (value != null)
				field.setValue(value);
			return field;
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			String cur = (String)field.getValue();
			try
			{
				syntax.validate(cur);
			} catch (IllegalAttributeValueException e)
			{
				field.setComponentError(new UserError(e.getMessage()));
				throw e;
			}
			return cur;
		}
	}

	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new EnumAttributeHandler();
	}

	@Override
	public String getValueAsString(String value, AttributeValueSyntax<String> syntax, int limited)
	{
		return TextOnlyAttributeHandler.trimString(value.toString(), limited);
	}

	@Override
	public Resource getValueAsImage(String value, AttributeValueSyntax<String> syntax,
			int maxWidth, int maxHeight)
	{
		return null;
	}
	
}
