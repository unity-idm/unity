/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.preferences;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandler;

/**
 * Handler of SAML preferences.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class SamlPreferencesHandler implements PreferencesHandler
{
	private UnityMessageSource msg;
	
	@Autowired
	public SamlPreferencesHandler(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
	}

	@Override
	public String getPreferenceLabel()
	{
		return msg.getMessage("SAMLPreferences.label");
	}

	@Override
	public String getPreferenceId()
	{
		return SamlPreferences.ID;
	}

	@Override
	public PreferencesEditor getPreferencesEditor(String value)
	{
		SamlPreferences preferences = new SamlPreferences();
		preferences.setSerializedConfiguration(value);
		return new SamlPreferencesEditor(msg, preferences);
	}
}
