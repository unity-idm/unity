/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.unicore.samlidp.web.SamlUnicoreIdPWebEndpointFactory;
import pl.edu.icm.unity.unicore.samlidp.ws.SamlUnicoreIdPSoapEndpointFactory;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.preferences.PreferencesEditor;
import pl.edu.icm.unity.webui.common.preferences.PreferencesHandler;

/**
 * Handler of UNICORE SAML preferences.
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class SamlPreferencesWithETDHandler implements PreferencesHandler
{
	private final Set<String> SUPPORTED_ENDPOINTS = new HashSet<String>();
	private UnityMessageSource msg;
	private IdentitiesManagement idsMan;
	private AttributesManagement atsMan;
	private AttributeSyntaxFactoriesRegistry registry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	
	@Autowired
	public SamlPreferencesWithETDHandler(UnityMessageSource msg, IdentitiesManagement idsMan,
			AttributesManagement atsMan, AttributeSyntaxFactoriesRegistry registry, 
			AttributeHandlerRegistry attributeHandlerRegistry)
	{
		super();
		this.msg = msg;
		this.idsMan = idsMan;
		this.atsMan = atsMan;
		this.registry = registry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		SUPPORTED_ENDPOINTS.add(SamlUnicoreIdPSoapEndpointFactory.NAME);
		SUPPORTED_ENDPOINTS.add(SamlUnicoreIdPWebEndpointFactory.NAME);
	}

	@Override
	public String getPreferenceLabel()
	{
		return msg.getMessage("SamlUnicoreIdPWebUI.preferences");
	}

	@Override
	public String getPreferenceId()
	{
		return SamlPreferencesWithETD.ID;
	}

	@Override
	public PreferencesEditor getPreferencesEditor(String value)
	{
		SamlPreferencesWithETD preferences = new SamlPreferencesWithETD(registry);
		preferences.setSerializedConfiguration(value);
		return new SamlPreferencesWithETDEditor(msg, preferences, idsMan, atsMan, attributeHandlerRegistry);
	}

	@Override
	public Set<String> getSupportedEndpoints()
	{
		return SUPPORTED_ENDPOINTS;
	}
}
