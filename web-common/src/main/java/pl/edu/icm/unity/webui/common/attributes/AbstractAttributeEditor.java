/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;

import com.vaadin.ui.AbstractOrderedLayout;

/**
 * Base of the components allowing to edit an attribute. The values are displayed too, however may be 
 * presented in a simplified form.
 * <p>
 * This base class provides a common editor code so it is easy to wire the {@link ListOfEmbeddedElementsStub}
 * class to edit valuesof an attribute.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractAttributeEditor
{
	protected UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	
	public AbstractAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry)
	{
		this.msg = msg;
		this.registry = registry;
	}
	
	protected ListOfEmbeddedElementsStub<LabelledValue> getValuesPart(AttributeType at, String label, boolean required,
			AbstractOrderedLayout layout)
	{
		ListOfEmbeddedElementsStub<LabelledValue> ret = new ListOfEmbeddedElementsStub<LabelledValue>(msg, 
				new AttributeValueEditorAndProvider(at, label, required), 
				at.getMinElements(), at.getMaxElements(), false, layout);
		ret.setLonelyLabel(label);
		return ret;
	}

	
	protected class AttributeValueEditorAndProvider implements EditorProvider<LabelledValue>, Editor<LabelledValue>
	{
		private AttributeType at;
		private AttributeValueEditor<?> editor;
		private LabelledValue editedValue;
		private String baseLabel;
		private boolean required;
		
		public AttributeValueEditorAndProvider(AttributeType at, String label, boolean required)
		{
			this.at = at;
			this.baseLabel = label;
			this.required = required;
		}

		@Override
		public Editor<LabelledValue> getEditor()
		{
			return new AttributeValueEditorAndProvider(at, baseLabel, required);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public ComponentsContainer getEditorComponent(LabelledValue value, int position)
		{
			if (value == null)
				value = new LabelledValue(null, establishLabel(position));

			WebAttributeHandler handler = registry.getHandler(at.getValueType().getValueSyntaxId());
			editor = handler.getEditorComponent(value.getValue(), value.getLabel(), at.getValueType());
			editedValue = value;
			return editor.getEditor(required);
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
				if (at.getMaxElements() > 1)
					return "(" + (position+1) +")";
				else
					return "";
			}
			
			if (at.getMaxElements() > 1)
			{
				String base = (baseLabel.endsWith(":")) ? baseLabel.substring(0, baseLabel.length()-1) 
						: baseLabel;
				return base +" (" + (position+1) +"):";
			} else
				return baseLabel;
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
