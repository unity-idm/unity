/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.ui.Component;

/**
 * Dialog allowing to edit a credential requirement. It takes an editor component 
 * as argument, so can be easily used to display edit dialog for an existing CR or CR creation dialog.
 * @author K. Benedyczak
 */
public class CredentialRequirementEditDialog extends AbstractDialog
{
	private CredentialRequirementEditor editor;
	private Callback callback;
	
	public CredentialRequirementEditDialog(UnityMessageSource msg, String caption, 
			CredentialRequirementEditor attributeEditor, Callback callback)
	{
		super(msg, caption);
		this.editor = attributeEditor;
		this.callback = callback;
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
			CredentialRequirements cr = editor.getCredentialRequirements();
			if (callback.newCredentialRequirement(cr))
				close();
		} catch (IllegalCredentialException e) 
		{
			NotificationPopup.showFormError(msg);
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newCredentialRequirement(CredentialRequirements cr);
	}
}
