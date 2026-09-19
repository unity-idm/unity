/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;

import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeModyficationEvent;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeSyntaxEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeValueEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandlerFactory;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.JsonAttributeSyntax;

public class JsonAttributeHandler implements WebAttributeHandler
{
	private MessageSource msg;
	private final JsonAttributeSyntax syntax;

	JsonAttributeHandler(JsonAttributeSyntax syntax, MessageSource msg)
	{
		this.syntax = syntax;
		this.msg = msg;
	}

	private static class JsonSyntaxEditor implements AttributeSyntaxEditor<JsonNode>
	{
		@Override
		public Optional<Component> getEditor()
		{
			return Optional.empty();
		}

		@Override
		public AttributeValueSyntax<JsonNode> getCurrentValue() throws IllegalAttributeTypeException
		{
			return new JsonAttributeSyntax();
		}
	}

	@org.springframework.stereotype.Component
	static class JsonAttributeHandlerFactory implements WebAttributeHandlerFactory
	{

		private final MessageSource msg;

		@Autowired
		public JsonAttributeHandlerFactory(MessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return JsonAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new JsonAttributeHandler((JsonAttributeSyntax) syntax, msg);
		}

		@Override
		public AttributeSyntaxEditor<JsonNode> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
		{
			return new JsonSyntaxEditor();
		}
	}

	private class JsonValueEditor implements AttributeValueEditor
	{
		private final JsonNode value;
		private String label;
		private final AttributeValueSyntax<JsonNode> syntax;
		private TextArea field;
		private SingleStringFieldBinder binder;
		private AttributeEditContext context;

		public JsonValueEditor(JsonNode value, String label, AttributeValueSyntax<JsonNode> syntax)
		{
			this.value = value;
			this.syntax = syntax;
			this.label = label;

		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{

			binder = new SingleStringFieldBinder(msg);
			this.context = context;

			boolean required = context.isRequired();

			field = new TextArea();
			field.setWidthFull();
			field.setTitle("");
			if (context.getValueChangeMode() != null)
			{
				field.setValueChangeMode(context.getValueChangeMode());
			}

			setLabel(label);

			if (label != null)
				field.setId("ValueEditor." + label);

			if (context.isCustomWidth())
			{
				if (!context.isCustomWidthAsString())
				{
					field.getElement()
							.getStyle()
							.set("width", context.getCustomWidth() + context.getCustomWidthUnit()
									.getSymbol());
				} else
				{
					field.getElement()
							.getStyle()
							.set("width", context.getCustomWidthAsString());

				}
			}

			binder.forField(field, required)
					.withValidator(this::validate)
					.bind("value");
			binder.setBean(new StringBindingValue(value == null ? "" : syntax.convertToString(value)));
			field.addValueChangeListener(e -> WebSession.getCurrent()
					.getEventBus()
					.fireEvent(new AttributeModyficationEvent()));

			return new ComponentsContainer(field);

		}

		private ValidationResult validate(Object value, ValueContext context)
		{
			String val = (String) value;

			try
			{
				syntax.validateStringValue(val);
				return ValidationResult.ok();
			} catch (Exception e)
			{
				return ValidationResult.error(e.getMessage());
			}
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			if (!binder.isValid())
			{
				binder.validate();
				throw new IllegalAttributeValueException("");
			}
			return binder.getBean()
					.getValue();
		}

		@Override
		public void setLabel(String label)
		{
			if (field.getParent()
					.orElse(null) instanceof FormLayout.FormItem)
				return;
			if (context.isShowLabelInline())
				field.getElement()
						.setProperty("placeholder", label);
			else
				field.getElement()
						.setProperty("label", label);
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

	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		JsonNode value = syntax.convertFromString(initialValue);
		return new JsonValueEditor(value, label, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new VerticalLayout();
	}

}
