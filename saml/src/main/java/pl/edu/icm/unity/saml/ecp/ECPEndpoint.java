/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToSPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.sp.SAMLResponseConsumerServlet;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * ECP endpoint used to enable ECP support in Unity. The endpoint doesn't use any authenticator by itself.
 * @author K. Benedyczak
 */
public class ECPEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	private Properties properties;
	private SAMLECPProperties samlProperties;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private RemoteMetaManager myMetadataManager;
	private MetaDownloadManager downloadManager;
	private String servletPath;
	private PKIManagement pkiManagement;
	private ECPContextManagement samlContextManagement;
	private URL baseAddress;
	private ReplayAttackChecker replayAttackChecker;
	private IdentityResolver identityResolver;
	private TranslationProfileManagement profileManagement;
	private InputTranslationEngine trEngine;
	private TokensManagement tokensMan;
	private IdentitiesManagement identitiesMan;
	private SessionManagement sessionMan;
	private UnityServerConfiguration mainCfg;
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	private MultiMetadataServlet metadataServlet;
	
	public ECPEndpoint(EndpointTypeDescription type, String servletPath,
			PKIManagement pkiManagement, ECPContextManagement samlContextManagement,
			URL baseAddress, String baseContext,
			ReplayAttackChecker replayAttackChecker, IdentityResolver identityResolver,
			TranslationProfileManagement profileManagement,
			InputTranslationEngine trEngine, TokensManagement tokensMan,
			IdentitiesManagement identitiesMan, SessionManagement sessionMan,
			Map<String, RemoteMetaManager> remoteMetadataManagers,
			UnityServerConfiguration mainCfg, MetaDownloadManager downloadManager,
			ExecutorsService executorsService, MultiMetadataServlet metadataServlet)
	{
		super(type);
		this.pkiManagement = pkiManagement;
		this.servletPath = servletPath;
		this.samlContextManagement = samlContextManagement;
		this.baseAddress = baseAddress;
		this.replayAttackChecker = replayAttackChecker;
		this.identityResolver = identityResolver;
		this.profileManagement = profileManagement;
		this.trEngine = trEngine;
		this.tokensMan = tokensMan;
		this.identitiesMan = identitiesMan;
		this.sessionMan = sessionMan;
		this.remoteMetadataManagers = remoteMetadataManagers;
		this.downloadManager = downloadManager;
		this.mainCfg = mainCfg;
		this.executorsService = executorsService;
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
		this.metadataServlet = metadataServlet;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(serializedState));
			samlProperties = new SAMLECPProperties(properties, pkiManagement);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the SAML ECP" +
					" endpoint's configuration", e);
		}
		
		if (samlProperties.getBooleanValue(SamlProperties.PUBLISH_METADATA))
			exposeMetadata();
		String myId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		if (!remoteMetadataManagers.containsKey(myId))
		{
			myMetadataManager = new RemoteMetaManager(samlProperties, 
					mainCfg, executorsService, pkiManagement, new MetaToSPConfigConverter(pkiManagement), downloadManager, SAMLECPProperties.IDPMETA_PREFIX);
			remoteMetadataManagers.put(myId, myMetadataManager);
			myMetadataManager.start();
		} else
			myMetadataManager = remoteMetadataManagers.get(myId);
	}

	private void exposeMetadata()
	{
		String metaPath = samlProperties.getValue(SAMLSPProperties.METADATA_PATH);
		IndexedEndpointType consumerEndpoint = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint.setIndex(1);
		consumerEndpoint.setBinding(SAMLConstants.BINDING_PAOS);
		consumerEndpoint.setLocation(responseConsumerAddress);
		consumerEndpoint.setIsDefault(true);

		IndexedEndpointType[] assertionConsumerEndpoints = new IndexedEndpointType[] {consumerEndpoint};
		MetadataProvider provider = MetadataProviderFactory.newSPInstance(samlProperties, 
				executorsService, assertionConsumerEndpoints, null);
		metadataServlet.addProvider("/" + metaPath, provider);
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize endpoint's configuration", e);
		}
		return writer.toString();
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		String endpointAddress = baseAddress.toExternalForm() + description.getContextAddress() +
				servletPath;
		ECPServlet ecpServlet = new ECPServlet(samlProperties, myMetadataManager, 
				samlContextManagement, endpointAddress, 
				replayAttackChecker, identityResolver, profileManagement, trEngine,
				tokensMan, pkiManagement, identitiesMan, sessionMan, 
				description.getRealm(), baseAddress.toExternalForm());
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath(description.getContextAddress());
		ServletHolder holder = new ServletHolder(ecpServlet);
		context.addServlet(holder, servletPath + "/*");
		return context;
	}
	
	@Override
	public void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
}
