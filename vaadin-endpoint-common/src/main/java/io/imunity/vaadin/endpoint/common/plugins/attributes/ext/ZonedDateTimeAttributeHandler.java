/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;

import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.*;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.attributes.NullAttributeValueException;
import pl.edu.icm.unity.stdext.attr.ZonedDateTimeAttributeSyntax;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;


class ZonedDateTimeAttributeHandler implements WebAttributeHandler
{

	private final MessageSource msg;

	private final ZonedDateTimeAttributeSyntax syntax;

	public ZonedDateTimeAttributeHandler(AttributeValueSyntax<?> syntax, MessageSource msg)
	{

		this.syntax = (ZonedDateTimeAttributeSyntax) syntax;
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
		return new ZonedDateTimeValueEditor(initialValue, label);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return AttributeHandlerHelper.getEmptySyntaxViewer(
				msg.getMessage("ZonedDateTimeAttributeHandler.info"));
	}

	private static class ZonedDateTimeSyntaxEditor
			implements AttributeSyntaxEditor<ZonedDateTime>
	{

		@Override
		public Optional<Component>  getEditor()
		{
			return Optional.empty();
		}

		@Override
		public AttributeValueSyntax<ZonedDateTime> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			return new ZonedDateTimeAttributeSyntax();
		}

	}

	private class ZonedDateTimeValueEditor implements AttributeValueEditor
	{
		private final String label;
		private final ZonedDateTime value;
		private DateTimePicker datetime;
		private ComboBox<String> zone;
		private AttributeEditContext context;
		
		public ZonedDateTimeValueEditor(String valueRaw, String label)
		{
			this.value = valueRaw == null ? null : syntax.convertFromString(valueRaw);
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			this.context = context;
			datetime = new DateTimePicker();
			zone = new ComboBox<>(msg.getMessage("ZonedDateTimeAttributeHandler.zone"), ZoneId.getAvailableZoneIds());
			zone.setValue(ZoneId.systemDefault().toString());
			zone.setRequired(true);
			setLabel(label);
			DatePicker.DatePickerI18n datePickerI18n = new DatePicker.DatePickerI18n();
			datePickerI18n.setDateFormat("yyyy-MM-dd HH:mm:ss");
			datetime.setDatePickerI18n(datePickerI18n);
			datetime.setRequiredIndicatorVisible(context.isRequired());
			if (value != null)
			{
				datetime.setValue(value.toLocalDateTime());
				zone.setValue(value.getZone().toString());
			}

			
			if (context.isCustomWidth())
			{
				if (!context.isCustomWidthAsString())
				{
					datetime.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
					zone.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
				} else
				{
					datetime.setWidth(context.getCustomWidthAsString());
					zone.setWidth(context.getCustomWidthAsString());
				}
			}
		
			datetime.addValueChangeListener(e -> WebSession.getCurrent().getEventBus().fireEvent(new AttributeModyficationEvent()));

			return new ComponentsContainer(datetime, zone);
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{

			if (!context.isRequired() && datetime.getValue() == null)
				return null;

			try
			{
				LocalDateTime cur = datetime.getValue();
				ZonedDateTime zoned = null;
				if (cur != null)
					zoned = ZonedDateTime.of(cur,
							ZoneId.of(zone.getValue()));
				syntax.validate(zoned);
				datetime.setInvalid(false);
				return syntax.convertToString(zoned);
			}
			catch (NullAttributeValueException e)
			{
				datetime.setInvalid(true);
				datetime.setErrorMessage(msg.getMessage("fieldRequired"));
				throw e;
			}
			catch (IllegalAttributeValueException e)
			{
				datetime.setInvalid(true);
				datetime.setErrorMessage(e.getMessage());
				throw e;
			} catch (Exception e)
			{
				datetime.setInvalid(true);
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
	static class DateAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;

		@Autowired
		DateAttributeHandlerFactory(MessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return ZonedDateTimeAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new ZonedDateTimeAttributeHandler(syntax, msg);
		}

		@Override
		public AttributeSyntaxEditor<ZonedDateTime> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new ZonedDateTimeSyntaxEditor();
		}
	}
}
