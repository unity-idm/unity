/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.edit;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

/**
 * Editing of a single attribute value inside of {@link ListOfEmbeddedElementsStub} 
 */
class InternalAttributeValueEditor implements Editor<LabelledValue>
{
	private AttributeHandlerRegistryV8 registry;
	private AttributeValueEditor editor;
	private LabelledValue editedValue;
	private String baseLabel;
	private AttributeEditContext editContext;
	
	public InternalAttributeValueEditor(AttributeHandlerRegistryV8 registry,
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
			if (!editContext.isShowLabelInline())
				base = base + ":";
			return base;
		} else
			return baseLabel;
	}
	
	static class Factory implements EditorProvider<LabelledValue>
	{
		private AttributeHandlerRegistryV8 registry;
		private String baseLabel;
		private AttributeEditContext editContext;

		Factory(AttributeHandlerRegistryV8 registry,
		        String baseLabel, AttributeEditContext editContext)
		{
			this.registry = registry;
			this.baseLabel = baseLabel;
			this.editContext = editContext;
		}

		@Override
		public Editor<LabelledValue> getEditor()
		{
			return new InternalAttributeValueEditor(registry, editContext, baseLabel);
		}
	}
}