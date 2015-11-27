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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServletFactoryImpl;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.IdPLoginController;
import pl.edu.icm.unity.server.api.internal.IdPLoginController.IdPLoginHandler;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
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
	
	private EndpointTypeDescription description;
	private ApplicationContext applicationContext;
	private FreemarkerHandler freemarkerHandler;
	private PKIManagement pkiManagement;
	private ExecutorsService executorsService;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private MetaDownloadManager downloadManager;
	private UnityServerConfiguration mainConfig;
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	private SLOReplyInstaller sloReplyInstaller;
	private IdpConsentDeciderServletFactoryImpl dispatcherServletFactory;
	private NetworkServer server;

	private UnityMessageSource msg;
	
	@Autowired
	public SamlIdPWebEndpointFactory(ApplicationContext applicationContext, FreemarkerHandler freemarkerHandler,
			PKIManagement pkiManagement, MetaDownloadManager downloadManager, 
			ExecutorsService executorsService, UnityServerConfiguration mainConfig,
			SAMLLogoutProcessorFactory logoutProcessorFactory, SLOReplyInstaller sloReplyInstaller,
			IdpConsentDeciderServletFactoryImpl dispatcherServletFactory,
			UnityMessageSource msg, NetworkServer server, IdPLoginController loginController)
	{
		this.applicationContext = applicationContext;
		this.freemarkerHandler = freemarkerHandler;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.msg = msg;
		this.server = server;
		this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<String, RemoteMetaManager>());
		this.downloadManager = downloadManager;
		this.mainConfig = mainConfig;
		this.logoutProcessorFactory = logoutProcessorFactory;
		this.sloReplyInstaller = sloReplyInstaller;
		this.dispatcherServletFactory = dispatcherServletFactory;
		
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
		loginController.addIdPLoginHandler(new IdPLoginHandlerImpl());
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new SamlAuthVaadinEndpoint(server, applicationContext, freemarkerHandler,
				SamlIdPWebUI.class, pkiManagement, executorsService, mainConfig,
				dispatcherServletFactory, remoteMetadataManagers, downloadManager,  
				logoutProcessorFactory, sloReplyInstaller, msg);
	}

	public static class IdPLoginHandlerImpl implements IdPLoginHandler
	{
		@Override
		public boolean isLoginInProgress()
		{
			return SAMLContextSupport.hasContext();
		}

		@Override
		public void breakLogin()
		{
			SAMLContextSupport.cleanContext();
		}
	}
}
