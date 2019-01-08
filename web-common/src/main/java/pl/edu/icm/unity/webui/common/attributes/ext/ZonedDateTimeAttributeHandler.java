/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.datefield.DateTimeResolution;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateTimeField;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.ZonedDateTimeAttributeSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;


/**
 * ZonedDateTime attribute handler for the web
 * 
 * @author P. Piernik
 */
public class ZonedDateTimeAttributeHandler implements WebAttributeHandler
{

	private UnityMessageSource msg;

	private ZonedDateTimeAttributeSyntax syntax;

	public ZonedDateTimeAttributeHandler(AttributeValueSyntax<?> syntax, UnityMessageSource msg)
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
		public Component getEditor()
		{
			return AttributeHandlerHelper.getEmptyEditor();
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
		private String label;
		private DateTimeField datetime;
		private ZonedDateTime value;
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
			datetime = new DateTimeField();
			zone = new ComboBox<>(msg.getMessage("ZonedDateTimeAttributeHandler.zone"), ZoneId.getAvailableZoneIds());
			zone.setSelectedItem(ZoneId.systemDefault().toString());
			zone.setEmptySelectionAllowed(false);
			datetime.setResolution(DateTimeResolution.SECOND);
			setLabel(label);
			datetime.setDateFormat("yyyy-MM-dd HH:mm:ss");
			datetime.setRequiredIndicatorVisible(context.isRequired());
			if (value != null)
			{
				datetime.setValue(value.toLocalDateTime());
				zone.setSelectedItem(value.getZone().toString());
			}

			if (context.isCustomWidth())
			{
				datetime.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
				zone.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			}
			
			ComponentsContainer ret = new ComponentsContainer(datetime, zone);
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
				ZonedDateTime zoned = null;
				if (cur != null)
					zoned = ZonedDateTime.of(cur,
							ZoneId.of(zone.getSelectedItem().get()));
				syntax.validate(zoned);
				datetime.setComponentError(null);
				return syntax.convertToString(zoned);
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
			if (context.isShowLabelInline())
				datetime.setPlaceholder(label);
			else
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
