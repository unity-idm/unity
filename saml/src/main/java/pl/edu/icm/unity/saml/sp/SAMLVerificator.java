/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.trust.SamlTrustChecker;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLHelper;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.metadata.LocalSPMetadataManager;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.SPRemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfigurationParser;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import pl.edu.icm.unity.saml.sp.web.IdPVisalSettings;
import pl.edu.icm.unity.types.authn.IdPInfo;
import pl.edu.icm.unity.types.authn.IdPInfo.IdpGroup;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;

/**
 * Binding irrelevant SAML logic: creation of a SAML authentication request and
 * verification of the answer.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class SAMLVerificator extends AbstractRemoteVerificator implements SAMLExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLVerificator.class);

	public static final String NAME = "saml2";
	public static final String METADATA_SERVLET_PATH = "/saml-sp-metadata";
	public static final String DESC = "Handles SAML assertions obtained from remote IdPs";

	private final pl.edu.icm.unity.saml.metadata.cfg.SPRemoteMetaManager.Factory remoteMetadataManagerFactory;
	private MultiMetadataServlet metadataServlet;
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	private Map<String, SPRemoteMetaManager> remoteMetadataManagers;
	private SPRemoteMetaManager myMetadataManager;
	private SLOSPManager sloManager;
	private SLOReplyInstaller sloReplyInstaller;
	private URIAccessService uriAccessService;

	private Map<String, LocalSPMetadataManager> localMetadataManagers;

	private final SAMLResponseVerificator responseVerificator;
	private final SAMLSPConfigurationParser configurationParser;

	private SAMLSPConfiguration spConfiguration;


	@Autowired
	public SAMLVerificator(RemoteAuthnResultTranslator processor, 
			ExecutorsService executorsService, SLOSPManager sloManager,
			SLOReplyInstaller sloReplyInstaller, SharedEndpointManagement sharedEndpointManagement,
			AdvertisedAddressProvider advertisedAddrProvider, URIAccessService uriAccessService,
			SAMLResponseVerificator responseVerificator,
			SAMLSPConfigurationParser configurationParser,
			SPRemoteMetaManager.Factory remoteMetadataManagerFactory)
	{
		super(NAME, DESC, SAMLExchange.ID, processor);
		this.executorsService = executorsService;
		this.sloManager = sloManager;
		this.sloReplyInstaller = sloReplyInstaller;
		this.uriAccessService = uriAccessService;
		this.responseVerificator = responseVerificator;
		this.configurationParser = configurationParser;
		this.remoteMetadataManagerFactory = remoteMetadataManagerFactory;

		URL baseAddress = advertisedAddrProvider.get();
		String baseContext = sharedEndpointManagement.getBaseContextPath();
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
	}

	private void init(Map<String, SPRemoteMetaManager> remoteMetadataManagers,
			Map<String, LocalSPMetadataManager> localMetadataManagers, MultiMetadataServlet metadataServlet)
	{
		this.remoteMetadataManagers = remoteMetadataManagers;
		this.localMetadataManagers = localMetadataManagers;
		this.metadataServlet = metadataServlet;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return configurationParser.serialize(spConfiguration);
	}

	/**
	 * Configuration in samlProperties is loaded, but it can be modified at runtime
	 * by the metadata manager. Therefore the source properties are used only to
	 * configure basic things (not related to trusted IDPs) while the virtual
	 * properties are used for authentication process setup.
	 */
	@Override
	public void setSerializedConfiguration(String source)
	{
		spConfiguration = configurationParser.parse(source);
		
		if (!localMetadataManagers.containsKey(instanceName))
		{
			LocalSPMetadataManager manager = new LocalSPMetadataManager(executorsService, responseConsumerAddress,
					sloManager, sloReplyInstaller, metadataServlet, uriAccessService);
			manager.updateConfiguration(spConfiguration);
			localMetadataManagers.put(instanceName, manager);
		} else
		{
			localMetadataManagers.get(instanceName).updateConfiguration(spConfiguration);
		}

		myMetadataManager = remoteMetadataManagers.containsKey(instanceName) ?
				remoteMetadataManagers.get(instanceName) : 
				remoteMetadataManagerFactory.getInstance();
		myMetadataManager.setBaseConfiguration(spConfiguration);
		if (!remoteMetadataManagers.containsKey(instanceName))
			remoteMetadataManagers.put(instanceName, myMetadataManager);

		try
		{
			initSLO();
		} catch (EngineException e)
		{
			throw new InternalException(
					"Can't initialize Single Logout subsystem for " + "the authenticator " + getName(), e);
		}
	}

	@Override
	public void destroy()
	{
		myMetadataManager.unregisterAll();
	}

	private void initSLO() throws EngineException
	{
		SamlTrustProvider samlTrustProvider = new SamlTrustProvider()
		{
			@Override
			public SamlTrustChecker getTrustChecker()
			{
				return spConfiguration.trustChecker;
			}

			@Override
			public Collection<SAMLEndpointDefinition> getSLOEndpoints(NameIDType samlId)
			{
				TrustedIdPKey idPConfigKey = getTrustedIdPs().getIdPConfigKey(samlId);
				if (idPConfigKey == null)
					return null;
				return getTrustedIdPs().get(idPConfigKey).logoutEndpoints;
			}

			@Override
			public List<PublicKey> getTrustedKeys(NameIDType samlEntity)
			{
				return getTrustedIdPs().getPublicKeysOfIdp(samlEntity.getStringValue());
			}
		};

		String sloPath = spConfiguration.sloPath;
		String sloRealm = spConfiguration.sloRealm;

		if (sloPath == null || sloRealm == null)
		{
			log.debug("Single Logout functionality will be disabled for SAML authenticator " + getName()
					+ " as its path and/or realm are/is undefined.");
			return;
		}

		String samlId = spConfiguration.requesterSamlId;
		X509Credential credential = spConfiguration.requesterCredential;
		IdentityTypeMapper idMapper = new IdentityTypeMapper(spConfiguration.effectiveMappings);
		sloManager.deployAsyncServlet(sloPath, idMapper, 600000, samlId, credential, samlTrustProvider, sloRealm);
		sloManager.deploySyncServlet(sloPath, idMapper, 600000, samlId, credential, samlTrustProvider, sloRealm);

		sloReplyInstaller.enable();
	}

	@Override
	public RemoteAuthnContext createSAMLRequest(TrustedIdPKey idpConfigKey, String servletPath,
			AuthenticationStepContext authnStepContext, LoginMachineDetails initialLoginMachine,
			String ultimateReturnURL, AuthenticationTriggeringContext triggeringContext)
	{
		RedirectedAuthnState baseState = new RedirectedAuthnState(authnStepContext, this::processResponse,
				initialLoginMachine, ultimateReturnURL, triggeringContext);
		
		TrustedIdPConfiguration idPConfiguration = getTrustedIdPs().get(idpConfigKey);
		if (!idPConfiguration.definitionComplete)
			throw new IllegalStateException("The selected IdP is not valid anymore, seems it was disabled");
		boolean sign = idPConfiguration.signRequest;
		String requesterId = spConfiguration.requesterSamlId; 
		String identityProviderURL = idPConfiguration.idpEndpointURL;
		String requestedNameFormat = idPConfiguration.requestedNameFormat;
		X509Credential credential = sign ? spConfiguration.requesterCredential : null;

		AuthnRequestDocument request = SAMLHelper.createSAMLRequest(responseConsumerAddress, sign, requesterId,
				identityProviderURL, requestedNameFormat, true, credential);
		return new RemoteAuthnContext(idPConfiguration, spConfiguration, baseState,
				request.xmlText(), request.getAuthnRequest().getID(), servletPath);
	}

	private AuthenticationResult processResponse(RedirectedAuthnState remoteAuthnState)
	{
		RemoteAuthnContext castedState = (RemoteAuthnContext) remoteAuthnState;
		TranslationProfile profile = castedState.getIdp().translationProfile;
		//TODO drop after moving to properties converter
//				getTranslationProfile(config,
//				idpKey + CommonWebAuthnProperties.TRANSLATION_PROFILE,
//				idpKey + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE);
		return responseVerificator.processResponse(remoteAuthnState, profile);
	}

	@Override
	public Set<TrustedIdPKey> getTrustedIdpKeysWithWebBindings()
	{
		return getTrustedIdPs().getKeys(); //TODO - we need to filter out IdPs w/o web binding. Or just filter them early?
	}
	
	@Override
	public TrustedIdPs getTrustedIdPs()
	{
		return myMetadataManager.getTrustedIdPs();
	}
	
	@Override
	public IdPVisalSettings getVisualSettings(TrustedIdPKey configKey, Locale locale)
	{
		TrustedIdPConfiguration trustedIdPConfiguration = myMetadataManager.getTrustedIdPs().get(configKey);
		if (trustedIdPConfiguration == null)
			throw new IllegalArgumentException("There is no IdP with key " + configKey);
		return new IdPVisalSettings(trustedIdPConfiguration.logoURI.getValue(locale.toLanguageTag()), 
				trustedIdPConfiguration.tags, 
				trustedIdPConfiguration.name.getValue(locale.toLanguageTag()));
	}

	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}

	@Override
	public List<IdPInfo> getIdPs()
	{
		List<IdPInfo> providers = new ArrayList<>();
		TrustedIdPs trustedIdPs = myMetadataManager.getTrustedIdPs();
		Collection<TrustedIdPConfiguration> idps = trustedIdPs.getAll();
		idps.forEach(idp ->
		{
			IdpGroup group = idp.federationId != null ? 
					new IdpGroup(idp.federationId, Optional.ofNullable(idp.federationName)) : null;

			if (idp.samlId != null)
			{
				providers.add(IdPInfo.builder()
						.withId(idp.samlId)
						.withConfigId(idp.key.toString())
						.withDisplayedName(idp.name)
						.withGroup(group).build());
			}
		});
		return providers;
	}

	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		private MultiMetadataServlet metadataServlet;
		private Map<String, SPRemoteMetaManager> remoteMetadataManagers;
		private Map<String, LocalSPMetadataManager> localSPMetadataManagers;

		@Autowired
		public Factory(ObjectFactory<SAMLVerificator> factory, SamlContextManagement contextManagement,
				SharedEndpointManagement sharedEndpointManagement,
				SharedRemoteAuthenticationContextStore sharedRemoteAuthenticationContextStore) throws EngineException
		{
			super(NAME, DESC, factory);

			ServletHolder servlet = new ServletHolder(
					new SAMLResponseConsumerServlet(contextManagement, sharedRemoteAuthenticationContextStore));
			sharedEndpointManagement.deployInternalEndpointServlet(SAMLResponseConsumerServlet.PATH, servlet, false);

			metadataServlet = new MultiMetadataServlet(METADATA_SERVLET_PATH);
			sharedEndpointManagement.deployInternalEndpointServlet(METADATA_SERVLET_PATH,
					new ServletHolder(metadataServlet), false);

			this.remoteMetadataManagers = Collections.synchronizedMap(new HashMap<>());
			this.localSPMetadataManagers = Collections.synchronizedMap(new HashMap<>());
		}

		@Override
		public CredentialVerificator newInstance()
		{
			SAMLVerificator ret = (SAMLVerificator) factory.getObject();
			ret.init(remoteMetadataManagers, localSPMetadataManagers, metadataServlet);
			return ret;
		}
	}
}
