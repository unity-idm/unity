/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;
import pl.edu.icm.unity.saml.idp.ws.SamlIdPSoapEndpointFactory;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
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
	private AttributeSyntaxFactoriesRegistry registry;
	private AttributeHandlerRegistry handlerReg;
	
	@Autowired
	public SamlPreferencesHandler(UnityMessageSource msg, IdentitiesManagement idsMan,
			AttributesManagement atsMan, AttributeSyntaxFactoriesRegistry registry,
			AttributeHandlerRegistry hadnlerReg)
	{
		super();
		this.msg = msg;
		this.idsMan = idsMan;
		this.atsMan = atsMan;
		this.registry = registry;
		this.handlerReg = hadnlerReg;
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
		SamlPreferences preferences = new SamlPreferences(registry);
		preferences.setSerializedConfiguration(value);
		return new SamlPreferencesEditor(msg, preferences, idsMan, atsMan, handlerReg);
	}

	@Override
	public Set<String> getSupportedEndpoints()
	{
		return SUPPORTED_ENDPOINTS;
	}
}
