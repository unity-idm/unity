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
	
	protected ListOfEmbeddedElements<?> getValuesPart(AttributeType at)
	{
		return new ListOfEmbeddedElements<>(msg, new AttributeValueEditorAndProvider(at), 
				at.getMinElements(), at.getMaxElements());
	}

	
	protected class AttributeValueEditorAndProvider implements EditorProvider<Object>, Editor<Object>
	{
		private AttributeType at;
		private AttributeValueEditor<?> editor;
		
		public AttributeValueEditorAndProvider(AttributeType at)
		{
			this.at = at;
		}

		@Override
		public Editor<Object> getEditor()
		{
			return new AttributeValueEditorAndProvider(at);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public Component getEditorComponent(Object value)
		{
			WebAttributeHandler handler = registry.getHandler(at.getValueType().getValueSyntaxId());
			editor = handler.getEditorComponent(value, at.getValueType());
			return editor.getEditor();
		}

		@Override
		public Object getValue() throws FormValidationException
		{
			try
			{
				return editor.getCurrentValue();
			} catch (IllegalAttributeValueException e)
			{
				throw new FormValidationException(e);
			}
		}
	}
}
