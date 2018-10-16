/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

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
		setSizeEm(40, 30);
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
		ret.setSpacing(true);
		ret.addComponent(new Label(msg.getMessage("CredentialReset.updateCredentialInfo")));

		VerticalLayout internal = new VerticalLayout();
		ComponentsContainer componentContainer = credEditor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(backend.getCredentialConfiguration())
				.withRequired(true)
				.withEntityId(backend.getEntityId())
				.build());
		internal.addComponents(componentContainer.getComponents());
		internal.setWidthUndefined();
		ret.addComponent(internal);
		ret.setComponentAlignment(internal, Alignment.MIDDLE_CENTER);
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
		} catch (MissingCredentialException mc)
		{
			return;
		} catch (IllegalCredentialException e)
		{
			NotificationPopup.showError(e.getMessage(), "");
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
