/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateTimeField;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.DateTimeAttributeSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

/**
 * DateTime attribute handler for the web
 * 
 * @author P. Piernik
 */
public class DateTimeAttributeHandler implements WebAttributeHandler
{

	private UnityMessageSource msg;
	private DateTimeAttributeSyntax syntax;

	public DateTimeAttributeHandler(AttributeValueSyntax<?> syntax, UnityMessageSource msg)
	{

		this.syntax = (DateTimeAttributeSyntax) syntax;
		this.msg = msg;

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

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new DateTimeValueEditor(initialValue, label);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return AttributeHandlerHelper
				.getEmptySyntaxViewer(msg.getMessage("DateTimeAttributeHandler.info"));
	}

	private static class DateTimeSyntaxEditor implements AttributeSyntaxEditor<LocalDateTime>
	{

		@Override
		public Component getEditor()
		{
			return AttributeHandlerHelper.getEmptyEditor();
		}

		@Override
		public AttributeValueSyntax<LocalDateTime> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			return new DateTimeAttributeSyntax();
		}

	}

	private class DateTimeValueEditor implements AttributeValueEditor
	{
		protected String label;
		private boolean required;
		private DateTimeField datetime;
		private LocalDateTime value;

		public DateTimeValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(boolean required, boolean adminMode)
		{
			this.required = required;
			datetime = new DateTimeField();
			datetime.setResolution(DateTimeResolution.SECOND);
			datetime.setCaption(label);
			datetime.setDateFormat("yyyy-MM-dd HH:mm:ss");
			if (value != null)
				datetime.setValue(value);
			ComponentsContainer ret = new ComponentsContainer(datetime);
			return ret;
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{

			if (datetime.getValue() == null)
			{
				if (!required)
				{
					return null;
				}
				datetime.setComponentError(
						new UserError(msg.getMessage("fieldRequired")));
				throw new IllegalAttributeValueException(
						msg.getMessage("fieldRequired"));
			}
			
			try
			{
				LocalDateTime cur = datetime.getValue();
				syntax.validate(cur);
				datetime.setComponentError(null);
				return syntax.convertToString(cur);
			} catch (IllegalAttributeValueException e)
			{
				datetime.setComponentError(new UserError(e.getMessage()));
				throw e;
			} catch (Exception e)
			{
				datetime.setComponentError(new UserError(e.getMessage()));
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}

		}

		@Override
		public void setLabel(String label)
		{
			datetime.setCaption(label);

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
			return DateTimeAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new DateTimeAttributeHandler(syntax, msg);
		}

		@Override
		public AttributeSyntaxEditor<LocalDateTime> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new DateTimeSyntaxEditor();
		}
	}
}
