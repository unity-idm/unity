/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.ws;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.idp.ws.SamlIdPSoapEndpointFactory;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Factory creating {@link SamlUnicoreSoapEndpoint} instances.
 * @author K. Benedyczak
 */
@Component
public class SamlUnicoreIdPSoapEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2unicoreidp-soap";
	public static final String NAME = "SAMLUnicoreSoapIdP";
	
	private EndpointTypeDescription description;
	private UnityMessageSource msg;
	private IdPEngine idpEngine;
	private PreferencesManagement preferencesMan;
	private PKIManagement pkiManagement;
	private ExecutorsService executorsService;
	private SessionManagement sessionMan;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private MetaDownloadManager downloadManager;
	private UnityServerConfiguration mainConfig;
	
	@Autowired
	public SamlUnicoreIdPSoapEndpointFactory(UnityMessageSource msg, IdPEngine idpEngine,
			PreferencesManagement preferencesMan, PKIManagement pkiManagement,
			ExecutorsService executorsService, SessionManagement sessionMan,
			MetaDownloadManager dowloadManager, UnityServerConfiguration mainConfig)
	{
		super();
		this.msg = msg;
		this.idpEngine = idpEngine;
		this.preferencesMan = preferencesMan;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.sessionMan = sessionMan;
		this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<String, RemoteMetaManager>());
		this.downloadManager = dowloadManager;
		this.mainConfig = mainConfig;

		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(WebServiceAuthentication.NAME);
		Map<String,String> paths=new HashMap<String, String>();
		paths.put(SERVLET_PATH,"SAML 2 UNICORE identity provider web endpoint");
		paths.put(SamlIdPSoapEndpointFactory.METADATA_SERVLET_PATH, 
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
		return new SamlUnicoreSoapEndpoint(msg, getDescription(), SERVLET_PATH,
				SamlIdPSoapEndpointFactory.METADATA_SERVLET_PATH, idpEngine,
				preferencesMan, pkiManagement, executorsService, sessionMan,
				remoteMetadataManagers, downloadManager, mainConfig);
	}
}
