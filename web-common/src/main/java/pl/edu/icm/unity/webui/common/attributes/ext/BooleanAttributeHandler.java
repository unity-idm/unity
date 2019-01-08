/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.BooleanAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;


/**
 * Boolean attribute handler for the web
 * @author K. Benedyczak
 */
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
			return new CompactFormLayout();
		}
	
		@Override
		public AttributeValueSyntax<Boolean> getCurrentValue() throws IllegalAttributeTypeException
		{
			return new BooleanAttributeSyntax();
		}
	}

	
	@org.springframework.stereotype.Component
	public static class BooleanAttributeHandlerFactory implements WebAttributeHandlerFactory
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
		private CheckBox field;
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
			field = new CheckBox();
			field.setValue(value);
			field.setCaption(label);
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
			field.setCaption(label);
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
