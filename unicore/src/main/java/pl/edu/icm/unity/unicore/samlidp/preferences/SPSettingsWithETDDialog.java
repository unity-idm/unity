/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;

/**
 * Shows {@link SPSettingsEditor} in a dialog.
 * @author K. Benedyczak
 */
public class SPSettingsWithETDDialog extends AbstractDialog
{
	private SPSettingsWithETDEditor editor;
	private Callback callback;
	
	public SPSettingsWithETDDialog(UnityMessageSource msg, SPSettingsWithETDEditor editor, Callback callback)
	{
		super(msg, msg.getMessage("SAMLPreferences.spDialogCaption"));
		defaultSizeUndfined = true;
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
		{
			ErrorPopup.showError(msg, msg.getMessage("Generic.formError"), 
					msg.getMessage("SAMLPreferences.spIsRequired"));
			return;
		}
		callback.updatedSP(editor.getSPSettings(), editor.getSPETDSettings(), sp);
		close();
	}
	
	public interface Callback
	{
		public void updatedSP(SPSettings spSettings, SPETDSettings etdSettings, String sp);
	}
}
