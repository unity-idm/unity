/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.datefield.DateResolution;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.DateAttributeSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

/**
 * Date attribute handler for the web
 * 
 * @author P. Piernik
 */
public class DateAttributeHandler implements WebAttributeHandler
{

	private UnityMessageSource msg;
	private DateAttributeSyntax syntax;

	public DateAttributeHandler(AttributeValueSyntax<?> syntax, UnityMessageSource msg)
	{

		this.syntax = (DateAttributeSyntax) syntax;
		this.msg = msg;

	}

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new DateValueEditor(initialValue, label);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return AttributeHandlerHelper.getEmptySyntaxViewer(msg.getMessage("DateAttributeHandler.info"));
	}

	private static class DateSyntaxEditor  implements AttributeSyntaxEditor<LocalDate>
	{

		@Override
		public Component getEditor()
		{ 
			return AttributeHandlerHelper.getEmptyEditor();
		}
		
		@Override
		public AttributeValueSyntax<LocalDate> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			return new DateAttributeSyntax();
		}
	}
	
	@Override
	public String getValueAsString(String value)
	{
		return AttributeHandlerHelper.getValueAsString(value);
	}

	@Override
	public Component getRepresentation(String value)
	{
		return AttributeHandlerHelper.getRepresentation(value);
	}
	
	private class DateValueEditor implements AttributeValueEditor
	{
		private String label;
		private boolean required;
		private DateField date;
		private LocalDate value;

		public DateValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(boolean required, boolean adminMode)
		{
			this.required = required;
			date = new DateField();
			date.setResolution(DateResolution.DAY);
			date.setCaption(label);	
			date.setDateFormat("yyyy-MM-dd");
			date.setRequiredIndicatorVisible(required);
			if (value != null)
				date.setValue(value);
			ComponentsContainer ret = new ComponentsContainer(date);
			return ret;
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{

			if (date.getValue() == null)
			{
				if (!required)
				{
					return null;
				}
				date.setComponentError(
						new UserError(msg.getMessage("fieldRequired")));
				throw new IllegalAttributeValueException(
						msg.getMessage("fieldRequired"));
			}

			try
			{
				LocalDate cur = date.getValue();
				syntax.validate(cur);
				date.setComponentError(null);
				return syntax.convertToString(cur);
			} catch (IllegalAttributeValueException e)
			{
				date.setComponentError(new UserError(e.getMessage()));
				throw e;
			} catch (Exception e)
			{
				date.setComponentError(new UserError(e.getMessage()));
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}

		}

		@Override
		public void setLabel(String label)
		{
			date.setCaption(label);

		}

	}

	@org.springframework.stereotype.Component
	public static class DateAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		@Autowired
		public DateAttributeHandlerFactory(UnityMessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return DateAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new DateAttributeHandler(syntax, msg);
		}

		@Override
		public AttributeSyntaxEditor<LocalDate> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new DateSyntaxEditor();
		}
	}
}
