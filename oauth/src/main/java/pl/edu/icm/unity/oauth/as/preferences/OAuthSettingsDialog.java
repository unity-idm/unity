/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.webui.common.AbstractDialog;

/**
 * Shows {@link OAuthSPSettingsEditor} in a dialog.
 * @author K. Benedyczak
 */
public class OAuthSettingsDialog extends AbstractDialog
{
	private OAuthSPSettingsEditor editor;
	private Callback callback;
	
	public OAuthSettingsDialog(MessageSource msg, OAuthSPSettingsEditor editor, Callback callback)
	{
		super(msg, msg.getMessage("OAuthPreferences.clientDialogCaption"));
		this.editor = editor;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		return editor;
	}

	@Override
	protected void onConfirm()
	{
		String sp = editor.getClient();
		if (sp == null)
			sp = "";
		callback.updatedClient(editor.getClientSettings(), sp);
		close();
	}
	
	public interface Callback
	{
		public void updatedClient(OAuthClientSettings clientSettings, String sp);
	}
}
