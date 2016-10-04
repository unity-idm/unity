/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.preferences;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
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
	private EntityManagement idsMan;
	private AttributeTypeManagement atsMan;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private IdentityTypeSupport idTpeSupport;
	
	@Autowired
	public SamlPreferencesWithETDHandler(UnityMessageSource msg, EntityManagement idsMan,
			AttributeTypeManagement atsMan, 
			AttributeHandlerRegistry attributeHandlerRegistry,
			IdentityTypeSupport idTpeSupport)
	{
		super();
		this.msg = msg;
		this.idsMan = idsMan;
		this.atsMan = atsMan;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.idTpeSupport = idTpeSupport;
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
		SamlPreferencesWithETD preferences = new SamlPreferencesWithETD();
		if (value != null)
			preferences.setSerializedConfiguration(JsonUtil.parse(value));
		return new SamlPreferencesWithETDEditor(msg, preferences, idsMan, atsMan, attributeHandlerRegistry,
				idTpeSupport);
	}

	@Override
	public Set<String> getSupportedEndpoints()
	{
		return SUPPORTED_ENDPOINTS;
	}
}
