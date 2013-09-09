/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 4th, last step of credential reset pipeline. In this dialog the user must provide the new credential.
 * 
 * @author K. Benedyczak
 */
public class CredentialResetFinalDialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	
	private CredentialEditor credEditor;
	
	public CredentialResetFinalDialog(UnityMessageSource msg, CredentialReset backend, 
			CredentialEditor credEditor)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("CredentialReset.updateCredential"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.defaultSizeUndfined = true;
		this.backend = backend;
		this.credEditor = credEditor;
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (CredentialResetStateVariable.get() != 3)
		{
			ErrorPopup.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		ret.addComponent(new Label(msg.getMessage("CredentialReset.updateCredentialInfo")));
		ret.addComponent(credEditor.getEditor(backend.getCredentialConfiguration()));
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
		if (CredentialResetStateVariable.get() != 3)
			throw new IllegalStateException("Wrong application security state in password reset!" +
					" This should never happen.");
		String updatedValue;
		try
		{
			updatedValue = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			return;
		}
		
		try
		{
			backend.updateCredential(updatedValue);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg.getMessage("CredentialReset.credentialInvalid"), e);
			return;
		}
		
		ErrorPopup.showNotice(msg.getMessage("notice"), msg.getMessage("CredentialReset.success"));
		close();
		CredentialResetStateVariable.reset();
	}
}
