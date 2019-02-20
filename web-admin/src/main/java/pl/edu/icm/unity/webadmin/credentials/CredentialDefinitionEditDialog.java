/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import com.vaadin.ui.Component;

import io.imunity.webadmin.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

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
			CredentialDefinition cd = editor.getCredentialDefinition();
			if (callback.newCredentialDefinition(cd, editor.getLocalCredState()))
				close();
		} catch (IllegalCredentialException e) 
		{
			NotificationPopup.showFormError(msg);
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newCredentialDefinition(CredentialDefinition cd, LocalCredentialState newState);
	}

}
