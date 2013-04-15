/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;

/**
 * Dialog allowing to edit an attribute value
 * @author K. Benedyczak
 * @param <T>
 */
public class ValueEditDialog<T> extends AbstractDialog
{
	private AttributeValueEditor<T> editor;
	private Callback<T> callback;
	
	public ValueEditDialog(UnityMessageSource msg, String caption, AttributeValueEditor<T> editor,
			Callback<T> callback)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setWidth(40, Unit.PERCENTAGE);
		setHeight(40, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents()
	{
		return editor.getEditor();
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			T value = editor.getCurrentValue();
			close();
			callback.updateValue(value);
		} catch (IllegalAttributeValueException e) 
		{
			return;
		}
	}
	
	public interface Callback<T>
	{
		public void updateValue(T newValue);
	}
}
