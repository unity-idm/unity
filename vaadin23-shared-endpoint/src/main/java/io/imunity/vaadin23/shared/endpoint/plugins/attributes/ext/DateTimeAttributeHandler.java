/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.*;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.DateTimeAttributeSyntax;

import java.time.LocalDateTime;


public class DateTimeAttributeHandler implements WebAttributeHandler
{

	private MessageSource msg;
	private DateTimeAttributeSyntax syntax;

	public DateTimeAttributeHandler(AttributeValueSyntax<?> syntax, MessageSource msg)
	{

		this.syntax = (DateTimeAttributeSyntax) syntax;
		this.msg = msg;

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
		private DateTimePicker datetime;
		private LocalDateTime value;
		private AttributeEditContext context;

		public DateTimeValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			this.context = context;
			datetime = new DateTimePicker();
			setLabel(label);
			DatePicker.DatePickerI18n datePickerI18n = new DatePicker.DatePickerI18n();
			datePickerI18n.setDateFormat("yyyy-MM-dd HH:mm:ss");
			datetime.setDatePickerI18n(datePickerI18n);
			datetime.setRequiredIndicatorVisible(context.isRequired());
			if (value != null)
				datetime.setValue(value);
			if (context.isCustomWidth())
				datetime.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			ComponentsContainer ret = new ComponentsContainer(datetime);
			return ret;
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{

			if (!context.isRequired() && datetime.getValue() == null)
				return null;
			
			try
			{
				LocalDateTime cur = datetime.getValue();
				syntax.validate(cur);
				datetime.setErrorMessage(null);
				return syntax.convertToString(cur);
			} catch (IllegalAttributeValueException e)
			{
				datetime.setErrorMessage(e.getMessage());
				throw e;
			} catch (Exception e)
			{
				datetime.setErrorMessage(e.getMessage());
				throw new IllegalAttributeValueException(e.getMessage(), e);
			}

		}

		@Override
		public void setLabel(String label)
		{
			if (context.isShowLabelInline())
				datetime.setDatePlaceholder(label);
			else
				datetime.setLabel(label);
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
