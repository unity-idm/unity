/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpointFactory;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandler;

/**
 * Handler of OAuth preferences.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class OAuthPreferencesHandler implements PreferencesHandler
{
	private final Set<String> SUPPORTED_ENDPOINTS = new HashSet<String>();
	private UnityMessageSource msg;
	private IdentitiesManagement idsMan;
	
	@Autowired
	public OAuthPreferencesHandler(UnityMessageSource msg, IdentitiesManagement idsMan)
	{
		super();
		this.msg = msg;
		this.idsMan = idsMan;
		SUPPORTED_ENDPOINTS.add(OAuthAuthzWebEndpointFactory.NAME);
	}

	@Override
	public String getPreferenceLabel()
	{
		return msg.getMessage("OAuthPreferences.label");
	}

	@Override
	public String getPreferenceId()
	{
		return OAuthPreferences.ID;
	}

	@Override
	public PreferencesEditor getPreferencesEditor(String value)
	{
		OAuthPreferences preferences = new OAuthPreferences();
		preferences.setSerializedConfiguration(value);
		return new OAuthPreferencesEditor(msg, preferences, idsMan);
	}

	@Override
	public Set<String> getSupportedEndpoints()
	{
		return SUPPORTED_ENDPOINTS;
	}
}
