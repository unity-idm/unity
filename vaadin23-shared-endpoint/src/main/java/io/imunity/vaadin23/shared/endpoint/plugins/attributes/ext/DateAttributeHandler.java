/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.*;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.DateAttributeSyntax;

import java.time.LocalDate;

public class DateAttributeHandler implements WebAttributeHandler
{

	private MessageSource msg;
	private DateAttributeSyntax syntax;

	public DateAttributeHandler(AttributeValueSyntax<?> syntax, MessageSource msg)
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
		return value;
	}

	@Override
	public Component getRepresentation(String value, AttributeViewerContext context)
	{
		return AttributeHandlerHelper.getRepresentation(value, context);
	}
	
	private class DateValueEditor implements AttributeValueEditor
	{
		private String label;
		private DatePicker date;
		private LocalDate value;
		private AttributeEditContext context;

		public DateValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			this.context = context;
			date = new DatePicker();
			date.setRequiredIndicatorVisible(context.isRequired());
			DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();
			i18n.setDateFormat("yyyy-MM-dd");
			date.setI18n(i18n);
			setLabel(label);
			if (value != null)
				date.setValue(value);
			if (context.isCustomWidth())
				date.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			ComponentsContainer ret = new ComponentsContainer(date);
			return ret;
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{

			if (!context.isRequired() && date.getValue() == null)
				return null;

			try
			{
				LocalDate cur = date.getValue();
				syntax.validate(cur);
				date.setErrorMessage(null);
				return syntax.convertToString(cur);
			} catch (IllegalAttributeValueException e)
			{
				date.setErrorMessage(e.getMessage());
				throw e;
			} catch (Exception e)
			{
				date.setErrorMessage(e.getMessage());
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}

		}

		@Override
		public void setLabel(String label)
		{
			if (context.isShowLabelInline())
				date.setPlaceholder(label);
			else
				date.setLabel(label);
		}

	}

	@org.springframework.stereotype.Component
	public static class DateAttributeHandlerFactoryV23 implements WebAttributeHandlerFactory
	{
		private MessageSource msg;

		@Autowired
		public DateAttributeHandlerFactoryV23(MessageSource msg)
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