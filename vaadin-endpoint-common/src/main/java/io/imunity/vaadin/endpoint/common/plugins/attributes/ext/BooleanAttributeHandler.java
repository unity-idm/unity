/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeModyficationEvent;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeSyntaxEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeValueEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.BooleanAttributeSyntax;


class BooleanAttributeHandler implements WebAttributeHandler
{
	private final BooleanAttributeSyntax syntax;

	BooleanAttributeHandler(BooleanAttributeSyntax syntax)
	{
		this.syntax = syntax;
	}

	private static class BooleanSyntaxEditor implements AttributeSyntaxEditor<Boolean>
	{
		@Override
		public Optional<Component>  getEditor()
		{
			return Optional.empty();
		}
	
		@Override
		public AttributeValueSyntax<Boolean> getCurrentValue() throws IllegalAttributeTypeException
		{
			return new BooleanAttributeSyntax();
		}
	}

	@org.springframework.stereotype.Component
	static class BooleanAttributeHandlerFactory implements WebAttributeHandlerFactory
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

	private static class BooleanValueEditor implements AttributeValueEditor
	{
		private final boolean value;
		private String label;
		private final AttributeValueSyntax<Boolean> syntax;
		private Checkbox field;

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
			boolean required = context.isRequired();
			field = new Checkbox();
			field.setValue(value);
			field.setLabel(label);
			field.setRequiredIndicatorVisible(required);
			if (label != null)
				field.setId("ValueEditor."+label);
			field.addValueChangeListener(e -> WebSession.getCurrent().getEventBus().fireEvent(new AttributeModyficationEvent()));
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
