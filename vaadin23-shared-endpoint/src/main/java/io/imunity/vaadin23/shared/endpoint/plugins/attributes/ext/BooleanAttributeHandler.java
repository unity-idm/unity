/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.*;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.BooleanAttributeSyntax;


public class BooleanAttributeHandler implements WebAttributeHandler
{
	private BooleanAttributeSyntax syntax;

	public BooleanAttributeHandler(BooleanAttributeSyntax syntax)
	{
		this.syntax = syntax;
	}

	private static class BooleanSyntaxEditor implements AttributeSyntaxEditor<Boolean>
	{
		@Override
		public Component getEditor()
		{
			return new FormLayout();
		}
	
		@Override
		public AttributeValueSyntax<Boolean> getCurrentValue() throws IllegalAttributeTypeException
		{
			return new BooleanAttributeSyntax();
		}
	}

	
	@org.springframework.stereotype.Component
	public static class BooleanAttributeHandlerFactoryV23 implements WebAttributeHandlerFactory
	{
		@Override
		public String getSupportedSyntaxId()
		{
			return BooleanAttributeSyntax.ID;
		}
		
		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new BooleanAttributeHandler((BooleanAttributeSyntax)syntax);
		}
		
		@Override
		public AttributeSyntaxEditor<Boolean> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
		{
			return new BooleanSyntaxEditor();
		}
	}

	private class BooleanValueEditor implements AttributeValueEditor
	{
		private boolean value;
		private String label;
		private AttributeValueSyntax<Boolean> syntax;
		private Checkbox field;
		private boolean required;
		
		public BooleanValueEditor(boolean value, String label, AttributeValueSyntax<Boolean> syntax)
		{
			this.value = value;
			this.syntax = syntax;
			this.label = label;
			if (label.endsWith(":"))
				this.label = label.substring(0, label.length()-1);
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			this.required = context.isRequired();
			field = new Checkbox();
			field.setValue(value);
			field.setLabel(label);
			field.setRequiredIndicatorVisible(this.required);
			if (label != null)
				field.setId("ValueEditor."+label);
			return new ComponentsContainer(field);
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			Boolean cur= field.getValue();
			return syntax.convertToString(cur);
		}

		@Override
		public void setLabel(String label)
		{
			if (label.endsWith(":"))
				label = label.substring(0, label.length()-1);
			field.setLabel(label);
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
		Boolean value = syntax.convertFromString(initialValue);
		return new BooleanValueEditor(value, label, syntax);
	}

	@Override
	public Component getSyntaxViewer()
	{
		return new VerticalLayout();
	}
}
