/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Factory creating {@link SamlSoapEndpoint} instances.
 * @author K. Benedyczak
 */
@Component
public class SamlIdPSoapEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2idp-soap";
	public static final String METADATA_SERVLET_PATH = "/metadata";
	public static final String NAME = "SAMLSoapIdP";
	
	private final EndpointTypeDescription description;
	private final UnityMessageSource msg;
	private final IdPEngine idpEngine;
	private final PreferencesManagement preferencesMan;
	private final PKIManagement pkiManagement;
	private final ExecutorsService executorsService;
	private final SessionManagement sessionMan;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private MetaDownloadManager downloadManager;
	private UnityServerConfiguration mainConfig;
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	private AuthenticationProcessor authnProcessor;
	private NetworkServer server;
	private AttributeTypeSupport aTypeSupport;
	
	@Autowired
	public SamlIdPSoapEndpointFactory(UnityMessageSource msg,
			PreferencesManagement preferencesMan, IdPEngine idpEngine,
			PKIManagement pkiManagement, ExecutorsService executorsService,
			SessionManagement sessionMan, MetaDownloadManager downloadManager,
			UnityServerConfiguration mainConfig, SAMLLogoutProcessorFactory logoutProcessorFactory,
			AuthenticationProcessor authnProcessor,
			NetworkServer server, AttributeTypeSupport aTypeSupport)
	{
		super();
		this.msg = msg;
		this.idpEngine = idpEngine;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.sessionMan = sessionMan;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.server = server;
		this.aTypeSupport = aTypeSupport;
		this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<String, RemoteMetaManager>());
		this.downloadManager = downloadManager;
		this.mainConfig = mainConfig;
		this.logoutProcessorFactory = logoutProcessorFactory;
		
		Set<String> supportedAuthn = new HashSet<String>();
		supportedAuthn.add(WebServiceAuthentication.NAME);
		Map<String,String> paths = new HashMap<String, String>();
		paths.put(SERVLET_PATH, "SAML 2 identity provider web endpoint");
		paths.put(METADATA_SERVLET_PATH, "Metadata of the SAML 2 identity provider web endpoint");
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
		return new SamlSoapEndpoint(msg, server, SERVLET_PATH,
				METADATA_SERVLET_PATH, idpEngine, preferencesMan, pkiManagement,
				executorsService, sessionMan, remoteMetadataManagers, downloadManager, mainConfig,
				logoutProcessorFactory, authnProcessor, aTypeSupport);
	}

}
