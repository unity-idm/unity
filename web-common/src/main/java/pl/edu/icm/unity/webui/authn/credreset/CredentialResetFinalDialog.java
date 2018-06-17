/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 6th, last step of credential reset pipeline. In this dialog the user must provide the new credential.
 * 
 * @author K. Benedyczak
 */
public class CredentialResetFinalDialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	
	private CredentialEditor credEditor;
	private int expectedState;
	
	public CredentialResetFinalDialog(UnityMessageSource msg, CredentialReset backend, 
			CredentialEditor credEditor, int expectedState)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("CredentialReset.updateCredential"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.expectedState = expectedState;
		setSize(40, 50);
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (CredentialResetStateVariable.get() != expectedState)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setSpacing(false);
		ret.addComponent(new Label(msg.getMessage("CredentialReset.updateCredentialInfo")));
		FormLayout internal = new FormLayout();
		internal.addComponents(credEditor.getEditor(false, 
				backend.getCredentialConfiguration(), true, backend.getEntityId(), false).getComponents());
		ret.addComponent(internal);
		return ret;
	}

	@Override
	protected void onCancel()
	{
		CredentialResetStateVariable.reset();
		super.onCancel();
	}
	
	@Override
	protected void onConfirm()
	{
		if (CredentialResetStateVariable.get() != expectedState)
			throw new IllegalStateException("Wrong application security state in credential reset!" +
					" This should never happen.");
		String updatedValue;
		try
		{
			updatedValue = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			NotificationPopup.showError(msg
					.getMessage("CredentialChangeDialog.credentialUpdateError"),
					e.getMessage());
			return;
		}
		
		try
		{
			backend.updateCredential(updatedValue);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialReset.credentialInvalid"), e);
			return;
		}
		
		NotificationPopup.showSuccess(msg.getMessage("notice"), msg.getMessage("CredentialReset.success"));
		close();
		CredentialResetStateVariable.reset();
	}
}
