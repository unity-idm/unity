/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.webui.common.AbstractDialog;

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
		setWidth(50, Unit.PERCENTAGE);
		setHeight(50, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents()
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		Attribute<?> attribute = editor.getAttribute();
		if (callback.newAttribute(attribute))
			close();
	}
	
	public interface Callback
	{
		public boolean newAttribute(Attribute<?> newAttribute);
	}
}
