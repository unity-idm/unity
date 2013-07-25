/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import pl.edu.icm.unity.samlidp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.samlidp.preferences.SamlSPSettingsViewer;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.unicore.samlidp.web.ETDSettingsEditor;

import com.vaadin.ui.Label;

/**
 * Shows a single {@link SPSettings} and {@link SPETDSettings}.
 * 
 * @author K. Benedyczak
 */
public class SamlSPSettingsWithETDViewer extends SamlSPSettingsViewer
{
	private Label etdSettings;
	
	public SamlSPSettingsWithETDViewer(UnityMessageSource msg)
	{
		super(msg);
		etdSettings = new Label();
		etdSettings.setCaption(msg.getMessage("SamlUnicoreIdPWebUI.etdSettings"));
		addComponents(etdSettings);
	}
	
	public void setInput(SPSettings spSettings, SPETDSettings spEtdSettings)
	{
		super.setInput(spSettings);
		
		if (spEtdSettings == null)
			return;
		
		String info = spEtdSettings.isGenerateETD() ? 
				msg.getMessage("SamlUnicoreIdPWebUI.etdValidity",
						spEtdSettings.getEtdValidity()/ETDSettingsEditor.MS_PER_DAY) : 
				msg.getMessage("SamlUnicoreIdPWebUI.etdDisallowed");
		etdSettings.setValue(info);
	}
}
