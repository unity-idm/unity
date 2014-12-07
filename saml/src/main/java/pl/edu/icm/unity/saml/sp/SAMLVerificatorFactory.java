/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import eu.unicore.samly2.validators.ReplayAttackChecker;

/**
 * Factory of {@link SAMLVerificator}s.
 * It also installs the {@link SAMLResponseConsumerServlet}.
 * @author K. Benedyczak
 */
@Component
public class SAMLVerificatorFactory implements CredentialVerificatorFactory
{
	public static final String NAME = "saml2";
	public static final String METADATA_SERVLET_PATH = "/saml-sp-metadata";
	
	private TranslationProfileManagement profileManagement;
	private InputTranslationEngine trEngine;
	private PKIManagement pkiMan;
	private UnityServerConfiguration mainConfig;
	private ReplayAttackChecker replayAttackChecker;
	private MultiMetadataServlet metadataServlet;
	private ExecutorsService executorsService;
	private URL baseAddress;
	private String baseContext;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private MetaDownloadManager downloadManager;
	private SLOSPManager sloManager;
	private SLOReplyInstaller sloReplyInstaller;
	
	@Autowired
	public SAMLVerificatorFactory(@Qualifier("insecure") TranslationProfileManagement profileManagement,
			InputTranslationEngine trEngine, 
			PKIManagement pkiMan, ReplayAttackChecker replayAttackChecker,
			SharedEndpointManagement sharedEndpointManagement, SamlContextManagement contextManagement,
			NetworkServer jettyServer, ExecutorsService executorsService, MetaDownloadManager downloadManager,
			UnityServerConfiguration mainConfig, SLOSPManager sloManager, 
			SLOReplyInstaller sloReplyInstaller) 
					throws EngineException
	{
		this.profileManagement = profileManagement;
		this.trEngine = trEngine;
		this.pkiMan = pkiMan;
		this.replayAttackChecker = replayAttackChecker;
		this.executorsService = executorsService;
		this.baseAddress = jettyServer.getAdvertisedAddress();
		this.baseContext = sharedEndpointManagement.getBaseContextPath();
		this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<String, RemoteMetaManager>());
		this.downloadManager = downloadManager;
		this.mainConfig = mainConfig;
		this.sloManager = sloManager;
		this.sloReplyInstaller = sloReplyInstaller;
		
		ServletHolder servlet = new ServletHolder(new SAMLResponseConsumerServlet(contextManagement));
		sharedEndpointManagement.deployInternalEndpointServlet(SAMLResponseConsumerServlet.PATH, servlet);
		
		metadataServlet = new MultiMetadataServlet(METADATA_SERVLET_PATH);
		sharedEndpointManagement.deployInternalEndpointServlet(METADATA_SERVLET_PATH, 
				new ServletHolder(metadataServlet));
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Handles SAML assertions obtained from remote IdPs";
	}

	@Override
	public CredentialVerificator newInstance()
	{
		return new SAMLVerificator(NAME, getDescription(), profileManagement, trEngine, pkiMan, 
				replayAttackChecker, executorsService, metadataServlet,
				baseAddress, baseContext, remoteMetadataManagers, downloadManager, mainConfig,
				sloManager, sloReplyInstaller);
	}
}
