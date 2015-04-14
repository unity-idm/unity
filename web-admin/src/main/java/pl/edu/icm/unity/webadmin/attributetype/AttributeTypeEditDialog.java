/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;

/**
 * Dialog allowing to edit an attribute type. It takes an editor component as argument, so can be easily used to display 
 * edit dialog for an existing attribute or attribute creation dialog.
 * @author K. Benedyczak
 */
public class AttributeTypeEditDialog extends AbstractDialog
{
	private AttributeTypeEditor editor;
	private Callback callback;
	
	public AttributeTypeEditDialog(UnityMessageSource msg, String caption, Callback callback, 
			AttributeTypeEditor attributeEditor)
	{
		super(msg, caption);
		this.editor = attributeEditor;
		this.callback = callback;
		setWidth(50, Unit.PERCENTAGE);
		setHeight(75, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents()
	{
		return editor.getComponent();
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			AttributeType attributeType = editor.getAttributeType();
			if (callback.newAttribute(attributeType))
				close();
		} catch (IllegalAttributeTypeException e) 
		{
			if (e.getMessage() == null || e.getMessage().equals(""))
				ErrorPopup.showError(msg, msg.getMessage("Generic.formError"), 
						msg.getMessage("Generic.formErrorHint"));
			else
				ErrorPopup.showError(msg, msg.getMessage("AttributeType.invalidSyntaxDefinition"), e);
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newAttribute(AttributeType newAttributeType);
	}
}
