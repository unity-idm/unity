/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Factory creating {@link SamlAuthVaadinEndpoint} endpoints with the UNICORE specific UI.
 * @author K. Benedyczak
 */
@Component
public class SamlUnicoreIdPWebEndpointFactory implements EndpointFactory
{
	public static final String SAML_CONSUMER_SERVLET_PATH = "/saml2unicoreIdp-web";
	public static final String SAML_UI_SERVLET_PATH = "/saml2unicoreIdp-web-ui";
	public static final String NAME = "SAMLUnicoreWebIdP";
	
	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	private FreemarkerHandler freemarkerHandler;
	private PKIManagement pkiManagement;
	protected ExecutorsService executorsService;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private UnityServerConfiguration mainConfig;

	
	@Autowired
	public SamlUnicoreIdPWebEndpointFactory(ApplicationContext applicationContext, 
			FreemarkerHandler freemarkerHandler, PKIManagement pkiManagement, 
			ExecutorsService executorsService, UnityServerConfiguration mainConfig)
	{
		this.applicationContext = applicationContext;
		this.freemarkerHandler = freemarkerHandler;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<String, RemoteMetaManager>());
		this.mainConfig = mainConfig;
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(VaadinAuthentication.NAME);
		Map<String,String> paths=new HashMap<String, String>();
		paths.put(SAML_CONSUMER_SERVLET_PATH,"SAML 2 UNICORE identity provider web endpoint");
		paths.put(SamlIdPWebEndpointFactory.SAML_META_SERVLET_PATH, 
				"Metadata of the SAML 2 identity provider web endpoint");
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 UNICORE identity provider web endpoint", supportedAuthn,paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new SamlAuthETDVaadinEndpoint(getDescription(), applicationContext,
				freemarkerHandler, SamlUnicoreIdPWebUI.class, SAML_UI_SERVLET_PATH,
				pkiManagement, executorsService, remoteMetadataManagers,
				mainConfig, SAML_CONSUMER_SERVLET_PATH,
				SamlIdPWebEndpointFactory.SAML_META_SERVLET_PATH);
	}
}
