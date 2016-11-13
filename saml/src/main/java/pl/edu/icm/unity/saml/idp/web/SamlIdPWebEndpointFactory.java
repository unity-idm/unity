/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating {@link SamlAuthVaadinEndpoint} endpoints.
 * @author K. Benedyczak
 */
@Component
public class SamlIdPWebEndpointFactory implements EndpointFactory
{
	public static final String NAME = "SAMLWebIdP";

	@Autowired
	private ObjectFactory<SamlAuthVaadinEndpoint> factory;
	
	private EndpointTypeDescription description;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;

	public SamlIdPWebEndpointFactory()
	{
		this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<>());
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(VaadinAuthentication.NAME);
		Map<String,String> paths = new HashMap<String, String>();
		paths.put(SamlAuthVaadinEndpoint.SAML_ENTRY_SERVLET_PATH, 
				"SAML 2 identity provider web endpoint");
		paths.put(SamlAuthVaadinEndpoint.SAML_META_SERVLET_PATH, 
				"Metadata of the SAML 2 identity provider web endpoint");
		paths.put(SamlAuthVaadinEndpoint.SAML_SLO_ASYNC_SERVLET_PATH, "Single Logout web endpoint "
				+ "(supports POST and Redirect bindings)");
		paths.put(SamlAuthVaadinEndpoint.SAML_SLO_SOAP_SERVLET_PATH, 
				"Single Logout web endpoint (supports SOAP binding)");
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 identity provider web endpoint", supportedAuthn, paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		SamlAuthVaadinEndpoint ret = factory.getObject();
		ret.init(remoteMetadataManagers);
		return ret;
	}
}
