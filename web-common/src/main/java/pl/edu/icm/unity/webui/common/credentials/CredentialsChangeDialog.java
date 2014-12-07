/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;


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
	private final boolean simpleMode;
	
	private CredentialsPanel ui;
	
	public CredentialsChangeDialog(UnityMessageSource msg, long entityId, AuthenticationManagement authnMan, 
			IdentitiesManagement idsMan, CredentialEditorRegistry credEditorReg, boolean simpleMode,
			Callback callback)
	{
		super(msg, msg.getMessage("CredentialChangeDialog.caption"), msg.getMessage("close"));
		this.authnMan = authnMan;
		this.idsMan = idsMan;
		this.entityId = entityId;
		this.credEditorReg = credEditorReg;
		this.callback = callback;
		this.simpleMode = simpleMode;
	}

	@Override
	protected Component getContents() throws Exception
	{
		try
		{
			ui = new CredentialsPanel(msg, entityId, authnMan, idsMan, credEditorReg, simpleMode);
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"), e);
			throw e;
		}
		
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
