/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.ws.authn.CXFAuthentication;

/**
 * Factory creating {@link SamlSoapEndpoint} instances.
 * @author K. Benedyczak
 */
@Component
public class SamlIdPSoapEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2idp-soap";
	public static final String NAME = "SAMLSoapIdP";
	
	private EndpointTypeDescription description;
	private UnityMessageSource msg;
	private IdentitiesManagement identitiesMan;
	private AttributesManagement attributesMan;
	private PreferencesManagement preferencesMan;
	
	
	@Autowired
	public SamlIdPSoapEndpointFactory(UnityMessageSource msg, IdentitiesManagement identitiesMan,
			AttributesManagement attributesMan, PreferencesManagement preferencesMan)
	{
		super();
		this.msg = msg;
		this.identitiesMan = identitiesMan;
		this.attributesMan = attributesMan;
		this.preferencesMan = preferencesMan;

		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(CXFAuthentication.NAME);
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 identity provider web endpoint", supportedAuthn);
	}

	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new SamlSoapEndpoint(msg, getDescription(), SERVLET_PATH, identitiesMan, 
				attributesMan, preferencesMan);
	}

}
