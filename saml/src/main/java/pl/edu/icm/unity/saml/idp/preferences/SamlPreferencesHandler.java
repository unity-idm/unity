/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;
import pl.edu.icm.unity.saml.idp.ws.SamlSoapEndpoint;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;
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
	private MessageSource msg;
	private EntityManagement idsMan;
	private AttributeTypeManagement atsMan;
	private AttributeHandlerRegistryV8 handlerReg;
	private IdentityTypeSupport idTypeSupport;
	
	@Autowired
	public SamlPreferencesHandler(MessageSource msg, EntityManagement idsMan,
	                              AttributeTypeManagement atsMan,
	                              AttributeHandlerRegistryV8 hadnlerReg, IdentityTypeSupport idTypeSupport)
	{
		super();
		this.msg = msg;
		this.idsMan = idsMan;
		this.atsMan = atsMan;
		this.handlerReg = hadnlerReg;
		this.idTypeSupport = idTypeSupport;
		SUPPORTED_ENDPOINTS.add(SamlSoapEndpoint.NAME);
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
		if (value != null)
			preferences.setSerializedConfiguration(JsonUtil.parse(value));
		return new SamlPreferencesEditor(msg, preferences, idsMan, atsMan, handlerReg, idTypeSupport);
	}

	@Override
	public Set<String> getSupportedEndpoints()
	{
		return SUPPORTED_ENDPOINTS;
	}
}
