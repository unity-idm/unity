/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
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
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

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
		private DateTimeField datetime;
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
			datetime = new DateTimeField();
			datetime.setResolution(DateTimeResolution.SECOND);
			setLabel(label);
			datetime.setDateFormat("yyyy-MM-dd HH:mm:ss");
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
