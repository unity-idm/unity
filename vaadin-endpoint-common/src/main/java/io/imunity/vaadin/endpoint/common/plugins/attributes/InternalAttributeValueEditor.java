/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.FormValidationException;

class InternalAttributeValueEditor implements ListOfEmbeddedElementsStub.Editor<LabelledValue>
{
	private AttributeHandlerRegistry registry;
	private AttributeValueEditor editor;
	private LabelledValue editedValue;
	private String baseLabel;
	private AttributeEditContext editContext;
	
	public InternalAttributeValueEditor(AttributeHandlerRegistry registry,
	                                    AttributeEditContext editContext, String label)
	{
		this.registry = registry;
		this.baseLabel = label;
		this.editContext = editContext;
	}

	@Override
	public ComponentsContainer getEditorComponent(LabelledValue value, int position)
	{
		if (value == null)
			value = new LabelledValue(null, establishLabel(position));

		AttributeValueSyntax<?> syntax = registry.getaTypeSupport()
				.getSyntax(editContext.getAttributeType());
		WebAttributeHandler handler = registry.getHandler(syntax);
		editor = handler.getEditorComponent(value.getValue(), value.getLabel());
		editedValue = value;
		ComponentsContainer ret = editor.getEditor(editContext);
		return ret;
	}

	@Override
	public LabelledValue getValue() throws FormValidationException
	{
		try
		{
			return new LabelledValue(editor.getCurrentValue(), editedValue.getLabel());
		} catch (IllegalAttributeValueException e)
		{
			throw new FormValidationException(e);
		}
	}

	@Override
	public void setEditedComponentPosition(int position)
	{
		editor.setLabel(establishLabel(position));
	}
	
	private String establishLabel(int position)
	{
		if (baseLabel == null)
		{
			if (editContext.getAttributeType().getMaxElements() > 1)
				return "(" + (position+1) +")";
			else
				return "";
		}
		
		if (editContext.getAttributeType().getMaxElements() > 1)
		{
			String base = (baseLabel.endsWith(":")) ? baseLabel.substring(0, baseLabel.length()-1) 
					: baseLabel;
			base = base +" (" + (position+1) +")";
			return base;
		} else
			return baseLabel;
	}
	
	static class Factory implements ListOfEmbeddedElementsStub.EditorProvider<LabelledValue>
	{
		private AttributeHandlerRegistry registry;
		private String baseLabel;
		private AttributeEditContext editContext;

		Factory(AttributeHandlerRegistry registry,
		        String baseLabel, AttributeEditContext editContext)
		{
			this.registry = registry;
			this.baseLabel = baseLabel;
			this.editContext = editContext;
		}

		@Override
		public ListOfEmbeddedElementsStub.Editor<LabelledValue> getEditor()
		{
			return new InternalAttributeValueEditor(registry, editContext, baseLabel);
		}
	}
}