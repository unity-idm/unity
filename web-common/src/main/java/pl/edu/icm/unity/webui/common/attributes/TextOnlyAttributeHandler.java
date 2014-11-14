/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;

import com.google.common.html.HtmlEscapers;
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
	private static final int SMALL_STRING = 50;
	
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
	public AttributeValueEditor<T> getEditorComponent(T initialValue, String label,
			AttributeValueSyntax<T> syntax)
	{
		return new StringValueEditor(initialValue, label, syntax);
	}
	
	private class StringValueEditor implements AttributeValueEditor<T>
	{
		private T value;
		private String label;
		private AttributeValueSyntax<T> syntax;
		private AbstractTextField field;
		private boolean required;
		
		public StringValueEditor(T value, String label, AttributeValueSyntax<T> syntax)
		{
			this.value = value;
			this.syntax = syntax;
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(boolean required)
		{
			this.required = required;
			boolean large = false;
			Integer limitedWidth = null;
			if (syntax instanceof StringAttributeSyntax)
			{
				StringAttributeSyntax sas = (StringAttributeSyntax) syntax;
				if (sas.getMaxLength() > LARGE_STRING)
					large = true;
				if (sas.getMaxLength() < SMALL_STRING)
					limitedWidth = sas.getMaxLength();
			}
			
			field = large ? new TextArea() : new TextField();
			if (value != null)
				field.setValue(value.toString());
			if (limitedWidth != null)
				field.setColumns(limitedWidth);
			else
				field.setSizeFull();
			field.setCaption(label);
			field.setRequired(required);
			
			StringBuilder sb = new StringBuilder();
			for (String hint: getHints(syntax))
				sb.append(hint).append("<br>");
			field.setDescription(HtmlEscapers.htmlEscaper().escape(sb.toString()));
			if (label != null)
				field.setId("ValueEditor."+label);
			
			return new ComponentsContainer(field);
		}

		@Override
		public T getCurrentValue() throws IllegalAttributeValueException
		{
			if (!required && field.getValue().isEmpty())
				return null;
			try
			{
				T cur = convertFromString(field.getValue());
				syntax.validate(cur);
				field.setComponentError(null);
				return cur;
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

	protected abstract T convertFromString(String value);
	
	protected abstract List<String> getHints(AttributeValueSyntax<T> syntax);
	
	@Override
	public Component getSyntaxViewer(AttributeValueSyntax<T> syntaxR)
	{
		VerticalLayout ret = new VerticalLayout();
		for (String hint: getHints(syntaxR))
		{
			Label info = new Label(hint);
			ret.addComponent(info);
		}
		return ret;
	}
}
