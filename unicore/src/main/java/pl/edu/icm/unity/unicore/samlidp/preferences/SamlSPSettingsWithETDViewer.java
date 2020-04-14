/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.saml.idp.preferences.SamlSPSettingsViewer;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD.SPETDSettings;
import pl.edu.icm.unity.unicore.samlidp.web.ETDSettingsEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Shows a single {@link SPSettings} and {@link SPETDSettings}.
 * 
 * @author K. Benedyczak
 */
public class SamlSPSettingsWithETDViewer extends SamlSPSettingsViewer
{
	private Label etdSettings;
	
	public SamlSPSettingsWithETDViewer(MessageSource msg, AttributeHandlerRegistry attributeHandlerRegistry)
	{
		super(msg, attributeHandlerRegistry);
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
