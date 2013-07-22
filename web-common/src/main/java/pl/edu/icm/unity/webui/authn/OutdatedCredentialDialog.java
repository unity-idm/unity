/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog.Callback;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

/**
 * Simple dialog wrapping {@link CredentialsChangeDialog}. It is invoked for users logged with outdated
 * credential. User is informed about invalidated credential and can choose to change it or logout. 
 * After changing the credential user can only logout.  
 * @author K. Benedyczak
 */
public class OutdatedCredentialDialog extends AbstractDialog
{
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private CredentialEditorRegistry credEditorReg;
	
	public OutdatedCredentialDialog(UnityMessageSource msg, AuthenticationManagement authnMan,
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg)
	{
		super(msg, msg.getMessage("OutdatedCredentialDialog.caption"), 
				msg.getMessage("OutdatedCredentialDialog.accept"), 
				msg.getMessage("OutdatedCredentialDialog.cancel"));
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.credEditorReg = credEditorReg;
		this.defaultSizeUndfined = true;
	}

	@Override
	protected Component getContents() throws Exception
	{
		return new Label(msg.getMessage("OutdatedCredentialDialog.info"));
	}

	@Override
	protected void onConfirm()
	{
		WrappedSession vss = VaadinSession.getCurrent().getSession();
		AuthenticatedEntity ae = (AuthenticatedEntity) vss.getAttribute(WebSession.USER_SESSION_KEY);
		CredentialsChangeDialog dialog = new CredentialsChangeDialog(msg, 
				ae.getEntityId(), 
				authnMan, 
				idsMan, 
				credEditorReg, 
				new Callback()
				{
					
					@Override
					public void onClose(boolean changed)
					{
						afterCredentialUpdate();
					}
				});
		dialog.show();
	}

	@Override
	protected void onCancel()
	{
		AuthenticationProcessor.softLogout();
		close();
	}
	
	private void afterCredentialUpdate()
	{
		new AbstractDialog(msg,	msg.getMessage("OutdatedCredentialDialog.finalCaption"), 
				msg.getMessage("ok"))
		{
			{
				this.defaultSizeUndfined = true;
			}
			
			@Override
			protected void onConfirm()
			{
				OutdatedCredentialDialog.this.onCancel();
			}
			
			@Override
			protected Component getContents() throws Exception
			{
				return new Label(msg.getMessage("OutdatedCredentialDialog.finalInfo"));
			}
		}.show();
	}
}
