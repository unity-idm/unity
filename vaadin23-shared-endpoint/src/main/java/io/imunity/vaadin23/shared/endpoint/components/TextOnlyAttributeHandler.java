/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.components;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.AttributeValueEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext.AttributeHandlerHelper;
import io.imunity.vaadin23.shared.endpoint.forms.registration.SingleStringFieldBinder;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

import java.util.List;

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
		private String value;
		private String label;
		private AttributeValueSyntax<?> syntax;
		private AbstractSinglePropertyField<?, String> field;
		private boolean required;
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
			
			this.required = context.isRequired();
			this.context = context;
			boolean editWithTextArea = false;
			if (syntax instanceof StringAttributeSyntax)
			{
				StringAttributeSyntax sas = (StringAttributeSyntax) syntax;
				editWithTextArea = sas.isEditWithTextArea();
				this.required = required && sas.getMinLength() > 0;
			}
			
			field = editWithTextArea ? new TextArea() : new TextField();
			if (editWithTextArea)
				field.getElement().getStyle().set("width", "100%");
			setLabel(label);
			
			StringBuilder sb = new StringBuilder();
			for (String hint: getHints())
				sb.append(hint).append("<br>");
			field.getElement().setProperty("title", sb.toString());
			if (label != null)
				field.setId("ValueEditor."+label);
			
			if (context.isCustomWidth())
				field.getElement().getStyle().set("width", context.getCustomWidth() + context.getCustomWidthUnit().getSymbol());
			
			binder.forField(field, required)
				.withValidator(this::validate)
				.bind("value");
			binder.setBean(new StringBindingValue(value == null ? "" : value));
			
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
			} catch (IllegalAttributeValueException e)
			{
				return ValidationResult.error(e.getMessage());
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
			Label info = new Label(hint);
			ret.add(info);
		}
		return ret;
	}
}