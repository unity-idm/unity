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
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
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
	private AuthenticationProcessor authnProcessor;
	
	public OutdatedCredentialDialog(UnityMessageSource msg, AuthenticationManagement authnMan,
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg,
			AuthenticationProcessor authnProcessor)
	{
		super(msg, msg.getMessage("OutdatedCredentialDialog.caption"), 
				msg.getMessage("OutdatedCredentialDialog.accept"), 
				msg.getMessage("OutdatedCredentialDialog.cancel"));
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.credEditorReg = credEditorReg;
		this.authnProcessor = authnProcessor;
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
		LoginSession ls = (LoginSession) vss.getAttribute(LoginToHttpSessionBinder.USER_SESSION_KEY);
		CredentialsChangeDialog dialog = new CredentialsChangeDialog(msg, 
				ls.getEntityId(), 
				authnMan, 
				idsMan, 
				credEditorReg, 
				new Callback()
				{
					@Override
					public void onClose(boolean changed)
					{
						afterCredentialUpdate(changed);
					}
				});
		dialog.show();
	}

	@Override
	protected void onCancel()
	{
		close();
		authnProcessor.logoutAndRefresh(true);
	}
	
	private void afterCredentialUpdate(final boolean changed)
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
				close();
			}
			
			@Override
			protected Component getContents() throws Exception
			{
				String info = changed ? msg.getMessage("OutdatedCredentialDialog.finalInfo") :
					msg.getMessage("OutdatedCredentialDialog.finalInfoNotChanged");
				return new Label(info);
			}
		}.show();
	}
}
