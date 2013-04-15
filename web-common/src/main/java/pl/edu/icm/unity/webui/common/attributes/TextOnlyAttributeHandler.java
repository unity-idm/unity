/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Base attribute handler for the web. Renders as label, edit in text field. Extensions has to implement
 * only id and conversion from String to domain type.
 * @author K. Benedyczak
 */
public abstract class TextOnlyAttributeHandler<T> implements WebAttributeHandler<T>
{
	private static final int LARGE_STRING = 100;
	
	public static String trimString(String full, int limited)
	{
		if (limited<=0 || limited>full.length())
			return full;
		if (limited < 5)
			throw new IllegalArgumentException("Limit may not be smaller then 5");
		return full.substring(0, limited-5) + "[...]";
		
	}

	@Override
	public String getValueAsString(T value, AttributeValueSyntax<T> syntax, int limited)
	{
		return trimString(value.toString(), limited);
	}
	
	@Override
	public Resource getValueAsImage(T value, AttributeValueSyntax<T> syntax, int maxWidth, int maxHeight)
	{
		return null;
	}

	@Override
	public Component getRepresentation(T value, AttributeValueSyntax<T> syntax)
	{
		return new Label(value.toString(), ContentMode.PREFORMATTED);
	}
	
	@Override
	public AttributeValueEditor<T> getEditorComponent(T initialValue, 
			AttributeValueSyntax<T> syntax)
	{
		return new StringValueEditor(initialValue, syntax);
	}
	
	private class StringValueEditor implements AttributeValueEditor<T>
	{
		private T value;
		private AttributeValueSyntax<T> syntax;
		private AbstractTextField field;
		
		public StringValueEditor(T value, AttributeValueSyntax<T> syntax)
		{
			this.value = value;
			this.syntax = syntax;
		}

		@Override
		public Component getEditor()
		{
			VerticalLayout main = new VerticalLayout();
			boolean large = false;
			if (syntax instanceof StringAttributeSyntax)
			{
				StringAttributeSyntax sas = (StringAttributeSyntax) syntax;
				if (sas.getMaxLength() > LARGE_STRING)
					large = true;
			}
			
			field = large ? new TextArea() : new TextField();
			if (value != null)
				field.setValue(value.toString());
			field.setSizeFull();
			main.addComponent(field);
			
			for (String hint: getHints(syntax))
			{
				Label info = new Label(hint);
				main.addComponent(info);
			}
			
			main.setSpacing(true);
			return main;
		}

		@Override
		public T getCurrentValue() throws IllegalAttributeValueException
		{
			try
			{
				T cur = convertFromString(field.getValue());
				syntax.validate(cur);
				return cur;
			} catch (IllegalAttributeValueException e)
			{
				field.setComponentError(null);
				field.setComponentError(new UserError(e.getMessage()));
				throw e;
			} catch (Exception e)
			{
				field.setComponentError(null);
				field.setComponentError(new UserError(e.getMessage()));
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}
		}
	}

	protected abstract T convertFromString(String value);
	
	protected abstract List<String> getHints(AttributeValueSyntax<T> syntax);
}
