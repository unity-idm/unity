/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Factory of {@link ECPEndpoint}s.
 * @author K. Benedyczak
 */
@Component
public class ECPEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2-ecp";
	public static final String METADATA_SERVLET_PATH = "/metadata";
	public static final String NAME = "SAML-ECP";
	
	private EndpointTypeDescription description;
	private PKIManagement pkiManagement;
	private URL baseAddress;
	private SamlContextManagement samlContextManagement;
	
	@Autowired
	public ECPEndpointFactory(PKIManagement pkiManagement, NetworkServer jettyServer,
			SamlContextManagement samlContextManagement)
	{
		this.pkiManagement = pkiManagement;
		this.baseAddress = jettyServer.getAdvertisedAddress();
		this.samlContextManagement = samlContextManagement;
		Set<String> supportedAuthn = new HashSet<String>();
		Map<String,String> paths = new HashMap<String, String>();
		paths.put(SERVLET_PATH, "SAML 2 ECP authentication endpoint");
		paths.put(METADATA_SERVLET_PATH, "Metadata of the SAML ECP endpoint");
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 ECP authentication endpoint", supportedAuthn, paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new ECPEndpoint(description, SERVLET_PATH, pkiManagement, samlContextManagement, baseAddress);
	}

}
