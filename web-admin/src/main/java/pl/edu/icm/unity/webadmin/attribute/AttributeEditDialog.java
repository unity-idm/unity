/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;

import com.vaadin.ui.Component;

/**
 * Dialog allowing to edit an attribute. It takes an editor component as argument, so can be easily used to display 
 * edit dialog for an existing attribute or attribute creation dialog.
 * @author K. Benedyczak
 */
public class AttributeEditDialog extends AbstractDialog
{
	private AttributeEditor editor;
	private Callback callback;
	
	public AttributeEditDialog(UnityMessageSource msg, String caption, Callback callback, 
			AttributeEditor attributeEditor)
	{
		super(msg, caption);
		this.editor = attributeEditor;
		this.callback = callback;
		setSize(60, 50);
	}

	@Override
	protected Component getContents()
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		Attribute<?> attribute;
		try
		{
			attribute = editor.getAttribute();
			if (callback.newAttribute(attribute))
				close();
		} catch (FormValidationException e)
		{
			NotificationPopup.showFormError(msg);
		}
	}
	
	public interface Callback
	{
		public boolean newAttribute(Attribute<?> newAttribute);
	}
}
