/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.components;

import java.util.List;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;

import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeModyficationEvent;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeValueEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ext.AttributeHandlerHelper;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

public abstract class TextOnlyAttributeHandler implements WebAttributeHandler
{
	protected AttributeValueSyntax<?> syntax;
	protected MessageSource msg;
		
	public TextOnlyAttributeHandler(MessageSource msg, AttributeValueSyntax<?> syntax)
	{
		this.syntax = syntax;
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
		return new StringValueEditor(initialValue, label, syntax);
	}
	
	private class StringValueEditor implements AttributeValueEditor
	{
		private final String value;
		private final String label;
		private final AttributeValueSyntax<?> syntax;
		private AbstractSinglePropertyField<?, String> field;
		private AttributeEditContext context;
		private SingleStringFieldBinder binder;
		
		public StringValueEditor(String value, String label, AttributeValueSyntax<?> syntax)
		{
			this.value = value;
			this.syntax = syntax;
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			binder =  new SingleStringFieldBinder(msg);

			boolean required = context.isRequired();
			this.context = context;
			boolean editWithTextArea = false;
			if (syntax instanceof StringAttributeSyntax sas)
			{
				editWithTextArea = sas.isEditWithTextArea();
				required = required && sas.getMinLength() > 0;
			}

			if(editWithTextArea)
			{
				TextArea textArea = new TextArea();
				textArea.setWidthFull();
				textArea.setTitle("");
				if (context.getValueChangeMode() != null)
				{
					textArea.setValueChangeMode(context.getValueChangeMode());
				}
				field = textArea;
			}
			else
			{
				TextField textField = new TextField();
				textField.setTitle("");
				if (context.getValueChangeMode() != null)
				{
					textField.setValueChangeMode(context.getValueChangeMode());
				}
				
				field = textField;
			}
			setLabel(label);
			
			
			
			StringBuilder sb = new StringBuilder();
			for (String hint: getHints())
				sb.append(hint).append("\n");
			Tooltip.forComponent(field).setText(sb.toString());
			if (label != null)
				field.setId("ValueEditor."+label);
			
			if (context.isCustomWidth())
			{
				if (!context.isCustomWidthAsString())
				{
					field.getElement().getStyle().set("width", context.getCustomWidth() + context.getCustomWidthUnit().getSymbol());
				}else 
				{
					field.getElement().getStyle().set("width", context.getCustomWidthAsString());

				}
			}
			
			
			binder.forField(field, required)
				.withValidator(this::validate)
				.bind("value");
			binder.setBean(new StringBindingValue(value == null ? "" : value));
			field.addValueChangeListener(e -> WebSession.getCurrent().getEventBus().fireEvent(new AttributeModyficationEvent()));

			
			return new ComponentsContainer(field);
		}

		private ValidationResult validate(Object value, ValueContext context)
		{
			String val = (String) value;
			if (val.isEmpty())
				return ValidationResult.ok(); //fall back to default checking
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
			return binder.getBean().getValue();
		}

		@Override
		public void setLabel(String label)
		{
			if(field.getParent().orElse(null) instanceof FormLayout.FormItem)
				return;
			if (context.isShowLabelInline())
				field.getElement().setProperty("placeholder", label);
			else
				field.getElement().setProperty("label", label);
		}
	}

	protected abstract List<String> getHints();
	
	@Override
	public Component getSyntaxViewer()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(false);
		ret.setMargin(false);
		for (String hint: getHints())
		{
			Span info = new Span(hint);
			ret.add(info);
		}
		return ret;
	}
}
