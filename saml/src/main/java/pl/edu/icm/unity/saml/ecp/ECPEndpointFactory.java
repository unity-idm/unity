/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.SPRemoteMetaManager;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Factory of {@link ECPEndpoint}s.
 * @author K. Benedyczak
 */
@Component
public class ECPEndpointFactory implements EndpointFactory
{
	public static final String METADATA_SERVLET_PATH = "/metadata";
	public static final String SERVLET_PATH = "/saml2-ecp";
	public static final String NAME = "SAML-ECP";
	
	private final ObjectFactory<ECPEndpoint> factory;
	private final EndpointTypeDescription description;
	private final MultiMetadataServlet metadataServlet;
	private final Map<String, SPRemoteMetaManager> remoteMetadataManagersBySamlId;
	
	@Autowired
	public ECPEndpointFactory(SharedEndpointManagement sharedEndpointManagement,
			ObjectFactory<ECPEndpoint> factory) throws EngineException
	{
		this.factory = factory;
		this.description = initDescription();
		metadataServlet = new MultiMetadataServlet(METADATA_SERVLET_PATH);
		sharedEndpointManagement.deployInternalEndpointServlet(METADATA_SERVLET_PATH, 
				new ServletHolder(metadataServlet), false);
		this.remoteMetadataManagersBySamlId = Collections.synchronizedMap(new HashMap<>());
	}
	
	private static EndpointTypeDescription initDescription()
	{
		Map<String,String> paths = new HashMap<>();
		paths.put(SERVLET_PATH, "SAML 2 ECP authentication endpoint");
		paths.put(METADATA_SERVLET_PATH, "Metadata of the SAML ECP endpoint");
		return new EndpointTypeDescription(NAME, "SAML 2 ECP authentication endpoint", 
				WebServiceAuthentication.NAME, paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		ECPEndpoint endpoint = factory.getObject();
		endpoint.init(remoteMetadataManagersBySamlId, metadataServlet);
		return endpoint;
	}
}
