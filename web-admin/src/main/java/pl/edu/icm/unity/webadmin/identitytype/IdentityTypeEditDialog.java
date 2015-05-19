/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;

import com.vaadin.ui.Component;

/**
 * Dialog allowing to edit an identity type.
 * @author K. Benedyczak
 */
public class IdentityTypeEditDialog extends AbstractDialog
{
	private IdentityTypeEditor editor;
	private Callback callback;
	
	public IdentityTypeEditDialog(UnityMessageSource msg, String caption, Callback callback, 
			IdentityTypeEditor editor)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.MEDIUM);
	}

	@Override
	protected Component getContents()
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		IdentityType identityType;
		try
		{
			identityType = editor.getIdentityType();
		} catch (FormValidationException e)
		{
			return;
		}
		if (callback.updatedIdentityType(identityType))
			close();
	}
	
	public interface Callback
	{
		public boolean updatedIdentityType(IdentityType newIdentityType);
	}
}
