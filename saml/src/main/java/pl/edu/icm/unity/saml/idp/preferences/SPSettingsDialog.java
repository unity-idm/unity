/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

import com.vaadin.ui.Component;

/**
 * Shows {@link SPSettingsEditor} in a dialog.
 * @author K. Benedyczak
 */
public class SPSettingsDialog extends AbstractDialog
{
	private SPSettingsEditor editor;
	private Callback callback;
	
	public SPSettingsDialog(UnityMessageSource msg, SPSettingsEditor editor, Callback callback)
	{
		super(msg, msg.getMessage("SAMLPreferences.spDialogCaption"));
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
		String sp = editor.getSP();
		if (sp == null)
			sp = "";
		callback.updatedSP(editor.getSPSettings(), sp);
		close();
	}
	
	public interface Callback
	{
		public void updatedSP(SPSettings spSettings, String sp);
	}
}
