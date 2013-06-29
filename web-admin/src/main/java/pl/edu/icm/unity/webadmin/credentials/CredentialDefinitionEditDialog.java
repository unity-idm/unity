/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;

/**
 * Dialog displaying a UI to create or edit a {@link CredentialDefinition}.
 * @author K. Benedyczak
 */
public class CredentialDefinitionEditDialog extends AbstractDialog
{
	private CredentialDefinitionEditor editor;
	private Callback callback;
	
	public CredentialDefinitionEditDialog(UnityMessageSource msg, String caption, 
			CredentialDefinitionEditor attributeEditor, Callback callback)
	{
		super(msg, caption);
		this.editor = attributeEditor;
		this.callback = callback;
		this.defaultSizeUndfined = true;
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
			CredentialDefinition cd = editor.getCredentialDefinition();
			if (callback.newCredentialDefinition(cd, editor.getLocalAuthnState()))
				close();
		} catch (IllegalCredentialException e) 
		{
			ErrorPopup.showError(msg.getMessage("Generic.formError"), 
						msg.getMessage("Generic.formErrorHint"));
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newCredentialDefinition(CredentialDefinition cd, LocalAuthenticationState newState);
	}

}
