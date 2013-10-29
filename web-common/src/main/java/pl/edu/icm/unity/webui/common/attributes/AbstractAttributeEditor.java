/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements.EditorProvider;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;

/**
 * Component allowing to edit an attribute. The values are displayed too, however may be 
 * presented in a simplified form.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractAttributeEditor extends CustomComponent
{
	protected UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	
	public AbstractAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry)
	{
		this.msg = msg;
		this.registry = registry;
	}
	
	protected ListOfEmbeddedElements<LabelledValue> getValuesPart(AttributeType at, String label)
	{
		ListOfEmbeddedElements<LabelledValue> ret = new ListOfEmbeddedElements<>(msg, 
				new AttributeValueEditorAndProvider(at, label), 
				at.getMinElements(), at.getMaxElements(), false);
		ret.setLonelyLabel(label+":");
		return ret;
	}

	
	protected class AttributeValueEditorAndProvider implements EditorProvider<LabelledValue>, Editor<LabelledValue>
	{
		private AttributeType at;
		private AttributeValueEditor<?> editor;
		private LabelledValue editedValue;
		private String baseLabel;
		
		public AttributeValueEditorAndProvider(AttributeType at, String label)
		{
			this.at = at;
			this.baseLabel = label;
		}

		@Override
		public Editor<LabelledValue> getEditor()
		{
			return new AttributeValueEditorAndProvider(at, baseLabel);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Component getEditorComponent(LabelledValue value, int position)
		{
			if (value == null)
				value = new LabelledValue(null, establishLabel(position));

			WebAttributeHandler handler = registry.getHandler(at.getValueType().getValueSyntaxId());
			editor = handler.getEditorComponent(value.getValue(), value.getLabel(), at.getValueType());
			editedValue = value;
			return editor.getEditor();
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
			if (at.getMaxElements() > 1)
				return baseLabel + " (" + (position+1) +"):";
			else
				return baseLabel + ":";
		}
	}
	
	protected class LabelledValue
	{
		private Object value;
		private String label;
		
		public LabelledValue(Object value, String label)
		{
			this.value = value;
			this.label = label;
		}

		public Object getValue()
		{
			return value;
		}

		public void setValue(Object value)
		{
			this.value = value;
		}

		public String getLabel()
		{
			return label;
		}

		public void setLabel(String label)
		{
			this.label = label;
		}
	}
}
