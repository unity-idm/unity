/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointEE8Instance;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.SPRemoteMetaManager;
import pl.edu.icm.unity.saml.metadata.cfg.SPRemoteMetaManager.Factory;
import pl.edu.icm.unity.saml.sp.SAMLResponseConsumerServlet;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfigurationParser;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ECP endpoint used to enable ECP support in Unity. The endpoint doesn't use any authenticator by itself.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class ECPEndpoint extends AbstractWebEndpoint implements WebAppEndpointEE8Instance
{
	private final PKIManagement pkiManagement;
	private final ECPContextManagement samlContextManagement;
	private final ReplayAttackChecker replayAttackChecker;
	private final TokensManagement tokensMan;
	private final EntityManagement identitiesMan;
	private final SessionManagement sessionMan;
	private final ExecutorsService executorsService;
	private final SAMLSPConfigurationParser configurationParser;
	private final RemoteAuthnResultTranslator remoteAuthnProcessor;
	private final URIAccessService uriAccessService;
	private final Factory remoteMetadataManagerFactory;
	private final URL baseAddress;
	private final String responseConsumerAddress;
	
	private Properties properties;
	private SAMLECPProperties samlProperties;
	private Map<String, SPRemoteMetaManager> remoteMetadataManagersBySamlId;
	private SPRemoteMetaManager myMetadataManager;
	private MultiMetadataServlet metadataServlet;
	private SAMLSPConfiguration spConfiguration;
	
	@Autowired
	public ECPEndpoint(NetworkServer server,
			@Qualifier("insecure") PKIManagement pkiManagement,
			ECPContextManagement samlContextManagement,
			ReplayAttackChecker replayAttackChecker,
			RemoteAuthnResultTranslator remoteAuthnProcessor,
			TokensManagement tokensMan,
			EntityManagement identitiesMan,
			SessionManagement sessionMan,
			ExecutorsService executorsService,
			SharedEndpointManagement sharedEndpointManagement,
			URIAccessService uriAccessService,
			AdvertisedAddressProvider advertisedAddrProvider,
			SAMLSPConfigurationParser configurationParser,
			SPRemoteMetaManager.Factory remoteMetadataManagerFactory)
	{
		super(server, advertisedAddrProvider);
		this.pkiManagement = pkiManagement;
		this.samlContextManagement = samlContextManagement;
		this.configurationParser = configurationParser;
		this.remoteMetadataManagerFactory = remoteMetadataManagerFactory;
		this.baseAddress = advertisedAddrProvider.get();
		this.replayAttackChecker = replayAttackChecker;
		this.remoteAuthnProcessor = remoteAuthnProcessor;
		this.tokensMan = tokensMan;
		this.identitiesMan = identitiesMan;
		this.sessionMan = sessionMan;
		this.executorsService = executorsService;
		String baseContext = sharedEndpointManagement.getBaseContextPath();
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
		this.uriAccessService = uriAccessService;
	}

	public void init(Map<String, SPRemoteMetaManager> remoteMetadataManagersBySamlId, 
			MultiMetadataServlet metadataServlet)
	{
		this.remoteMetadataManagersBySamlId = remoteMetadataManagersBySamlId;
		this.metadataServlet = metadataServlet;
	}
	
	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		spConfiguration = configurationParser.parse(serializedState);
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
		
		if (spConfiguration.publishMetadata)
			exposeMetadata();
		String myId = spConfiguration.requesterSamlId;
		
		myMetadataManager = remoteMetadataManagersBySamlId.computeIfAbsent(myId, 
				key -> remoteMetadataManagerFactory.getInstance());
		myMetadataManager.setBaseConfiguration(spConfiguration);
	}

	@Override
	public void destroyOverridable()
	{
		myMetadataManager.unregisterAll();
	}
	
	private void exposeMetadata()
	{
		String metaPath = spConfiguration.metadataURLPath;
		IndexedEndpointType consumerEndpoint = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint.setIndex(1);
		consumerEndpoint.setBinding(SAMLConstants.BINDING_PAOS);
		consumerEndpoint.setLocation(responseConsumerAddress);
		consumerEndpoint.setIsDefault(true);

		IndexedEndpointType[] assertionConsumerEndpoints = new IndexedEndpointType[] {consumerEndpoint};
		MetadataProvider provider = MetadataProviderFactory.newSPInstance(spConfiguration, uriAccessService,
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
		String endpointAddress = baseAddress.toExternalForm() + description.getEndpoint().getContextAddress() +
				ECPEndpointFactory.SERVLET_PATH;
		
		ECPServlet ecpServlet = new ECPServlet(samlProperties.getJWTConfig(), 
				() -> spConfiguration,
				myMetadataManager, 
				samlContextManagement, endpointAddress, 
				replayAttackChecker, remoteAuthnProcessor,
				tokensMan, pkiManagement, identitiesMan, sessionMan, 
				description.getRealm(), baseAddress.toExternalForm());
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath(description.getEndpoint().getContextAddress());
		ServletHolder holder = new ServletHolder(ecpServlet);
		context.addServlet(holder, ECPEndpointFactory.SERVLET_PATH + "/*");
		return context;
	}
	
	@Override
	public void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
}
