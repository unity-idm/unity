/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import com.vaadin.ui.AbstractOrderedLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;

/**
 * Base of the components allowing to edit an attribute. The values are displayed too, however may be 
 * presented in a simplified form.
 * <p>
 * This base class provides a common editor code so it is easy to wire the {@link ListOfEmbeddedElementsStub}
 * class to edit values of an attribute.
 * 
 * @author K. Benedyczak
 */
public abstract class AbstractAttributeEditor
{
	protected UnityMessageSource msg;
	protected AttributeHandlerRegistry registry;
	
	public AbstractAttributeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry)
	{
		this.msg = msg;
		this.registry = registry;
	}
	
	protected ListOfEmbeddedElementsStub<LabelledValue> getValuesPart(AttributeType at, String label, 
			boolean required, boolean adminMode, AbstractOrderedLayout layout)
	{
		ListOfEmbeddedElementsStub<LabelledValue> ret = new ListOfEmbeddedElementsStub<LabelledValue>(msg, 
				new AttributeValueEditorAndProvider(at, label, required, adminMode), 
				at.getMinElements(), at.getMaxElements(), false, layout);
		ret.setLonelyLabel(label);
		return ret;
	}

	
	protected class AttributeValueEditorAndProvider implements EditorProvider<LabelledValue>, Editor<LabelledValue>
	{
		private AttributeType at;
		private AttributeValueEditor editor;
		private LabelledValue editedValue;
		private String baseLabel;
		private boolean required;
		private boolean adminMode;
		
		public AttributeValueEditorAndProvider(AttributeType at, String label, boolean required, 
				boolean adminMode)
		{
			this.at = at;
			this.baseLabel = label;
			this.required = required;
			this.adminMode = adminMode;
		}

		@Override
		public Editor<LabelledValue> getEditor()
		{
			return new AttributeValueEditorAndProvider(at, baseLabel, required, adminMode);
		}

		@Override
		public ComponentsContainer getEditorComponent(LabelledValue value, int position)
		{
			if (value == null)
				value = new LabelledValue(null, establishLabel(position));

			AttributeValueSyntax<?> syntax = registry.getaTypeSupport().getSyntax(at);
			WebAttributeHandler handler = registry.getHandler(syntax);
			editor = handler.getEditorComponent(value.getValue(), value.getLabel());
			editedValue = value;
			ComponentsContainer ret = editor.getEditor(required, adminMode, at.getName());
			String description = at.getDescription().getValue(msg);
			if (description != null && !description.equals(""))
				ret.setDescription(HtmlConfigurableLabel.conditionallyEscape(description));
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
		private String value;
		private String label;
		
		public LabelledValue(String value, String label)
		{
			this.value = value;
			this.label = label;
		}

		public String getValue()
		{
			return value;
		}

		public void setValue(String value)
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
