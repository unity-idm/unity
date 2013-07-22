/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;


/**
 * Allows to change a credential.
 * @author K. Benedyczak
 */
public class CredentialsChangeDialog extends AbstractDialog
{
	private AuthenticationManagement authnMan;
	private IdentitiesManagement idsMan;
	private CredentialEditorRegistry credEditorReg;
	private Callback callback;
	private final long entityId;
	
	private CredentialsPanel ui;
	
	public CredentialsChangeDialog(UnityMessageSource msg, long entityId, AuthenticationManagement authnMan, 
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg, Callback callback)
	{
		super(msg, msg.getMessage("CredentialChangeDialog.caption"), msg.getMessage("close"));
		this.defaultSizeUndfined = true;
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.entityId = entityId;
		this.credEditorReg = credEditorReg;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		ui = new CredentialsPanel(msg, entityId, authnMan, idsMan, credEditorReg);
		return ui;
	}

	@Override
	protected void onConfirm()
	{
		callback.onClose(ui.isChanged());
		close();
	}
	
	public interface Callback
	{
		public void onClose(boolean changed);
	}
}
