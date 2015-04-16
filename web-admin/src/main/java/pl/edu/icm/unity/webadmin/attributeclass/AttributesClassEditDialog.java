/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;

import com.vaadin.ui.Component;

/**
 * Dialog allowing to edit {@link AttributesClass}. It takes an editor component 
 * {@link AttributesClassEditor} as argument.
 * @author K. Benedyczak
 */
public class AttributesClassEditDialog extends AbstractDialog
{
	private AttributesClassEditor editor;
	private Callback callback;
	
	public AttributesClassEditDialog(UnityMessageSource msg, String caption, AttributesClassEditor editor, 
			Callback callback)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected Component getContents()
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			AttributesClass attributesClass = editor.getAttributesClass();
			if (callback.newAttributesClass(attributesClass))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), 
						msg.getMessage("Generic.formErrorHint"));
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newAttributesClass(AttributesClass newAC);
	}

}
