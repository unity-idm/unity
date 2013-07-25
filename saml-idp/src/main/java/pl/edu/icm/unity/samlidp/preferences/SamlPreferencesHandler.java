/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.preferences;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.samlidp.web.SamlIdPWebEndpointFactory;
import pl.edu.icm.unity.samlidp.ws.SamlIdPSoapEndpointFactory;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
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
	private final Set<String> SUPPORTED_ENDPOINTS = new HashSet<String>();
	private UnityMessageSource msg;
	private IdentitiesManagement idsMan;
	private AttributesManagement atsMan;
	
	@Autowired
	public SamlPreferencesHandler(UnityMessageSource msg, IdentitiesManagement idsMan,
			AttributesManagement atsMan)
	{
		super();
		this.msg = msg;
		this.idsMan = idsMan;
		this.atsMan = atsMan;
		SUPPORTED_ENDPOINTS.add(SamlIdPSoapEndpointFactory.NAME);
		SUPPORTED_ENDPOINTS.add(SamlIdPWebEndpointFactory.NAME);
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
		return new SamlPreferencesEditor(msg, preferences, idsMan, atsMan);
	}

	@Override
	public Set<String> getSupportedEndpoints()
	{
		return SUPPORTED_ENDPOINTS;
	}
}
