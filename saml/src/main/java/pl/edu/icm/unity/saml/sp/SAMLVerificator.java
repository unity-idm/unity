/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationException;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
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
import pl.edu.icm.unity.saml.SAMLResponseValidatorUtil;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.metadata.LocalSPMetadataManager;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToSPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.sp.web.IdPVisalSettings;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Binding irrelevant SAML logic: creation of a SAML authentication request and verification of the answer.
 * @author K. Benedyczak
 */
@PrototypeComponent
public class SAMLVerificator extends AbstractRemoteVerificator implements SAMLExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLVerificator.class);

	public static final String NAME = "saml2";
	public static final String METADATA_SERVLET_PATH = "/saml-sp-metadata";
	public static final String DESC = "Handles SAML assertions obtained from remote IdPs"; 
			
	private SAMLSPProperties samlProperties;
	private PKIManagement pkiMan;
	private MultiMetadataServlet metadataServlet;
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private RemoteMetaManager myMetadataManager;
	private ReplayAttackChecker replayAttackChecker;
	private SLOSPManager sloManager;
	private SLOReplyInstaller sloReplyInstaller;
	private RemoteMetadataService metadataService;
	private URIAccessService uriAccessService;
	
	private MessageSource msg;

	private Map<String, LocalSPMetadataManager> localMetadataManagers;
	
	@Autowired
	public SAMLVerificator(RemoteAuthnResultProcessor processor,
			@Qualifier("insecure") PKIManagement pkiMan,
			ReplayAttackChecker replayAttackChecker,
			ExecutorsService executorsService,
			RemoteMetadataService metadataService,
			SLOSPManager sloManager,
			SLOReplyInstaller sloReplyInstaller,
			MessageSource msg,
			SharedEndpointManagement sharedEndpointManagement,
			AdvertisedAddressProvider advertisedAddrProvider,
			URIAccessService uriAccessService)
	{
		super(NAME, DESC, SAMLExchange.ID, processor);
		this.metadataService = metadataService;
		this.pkiMan = pkiMan;
		this.executorsService = executorsService;
		this.msg = msg;
		this.replayAttackChecker = replayAttackChecker;
		this.sloManager = sloManager;
		this.sloReplyInstaller = sloReplyInstaller;
		this.uriAccessService = uriAccessService;

		URL baseAddress = advertisedAddrProvider.get();
		String baseContext = sharedEndpointManagement.getBaseContextPath();
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
	}

	private void init(Map<String, RemoteMetaManager> remoteMetadataManagers,
			Map<String, LocalSPMetadataManager> localMetadataManagers,
			MultiMetadataServlet metadataServlet)
	{
		this.remoteMetadataManagers = remoteMetadataManagers;
		this.localMetadataManagers = localMetadataManagers;
		this.metadataServlet = metadataServlet;
	}
	
	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			samlProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize SAML verificator configuration", e);
		}
		return sbw.toString();	
	}

	/**
	 * Configuration in samlProperties is loaded, but it can be modified at runtime by the metadata manager.
	 * Therefore the source properties are used only to configure basic things (not related to trusted IDPs)
	 * while the virtual properties are used for authentication process setup.
	 */
	@Override
	public void setSerializedConfiguration(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			samlProperties = new SAMLSPProperties(properties, pkiMan);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator(?)", e);
		}
		
		if (!localMetadataManagers.containsKey(instanceName))
		{
			LocalSPMetadataManager manager = new LocalSPMetadataManager(executorsService, 
					responseConsumerAddress, 
					sloManager, sloReplyInstaller, metadataServlet, uriAccessService);
			manager.updateConfiguration(samlProperties);
			localMetadataManagers.put(instanceName, manager);
		} else
		{
			localMetadataManagers.get(instanceName).updateConfiguration(samlProperties);
		}

		if (!remoteMetadataManagers.containsKey(instanceName))
		{
			myMetadataManager = new RemoteMetaManager(samlProperties, 
					pkiMan, 
					new MetaToSPConfigConverter(pkiMan, msg), 
					metadataService, SAMLSPProperties.IDPMETA_PREFIX);
			remoteMetadataManagers.put(instanceName, myMetadataManager);
		} else
		{
			myMetadataManager = remoteMetadataManagers.get(instanceName);
			myMetadataManager.setBaseConfiguration(samlProperties);
		}
		
		try
		{
			initSLO();
		} catch (EngineException e)
		{
			throw new InternalException("Can't initialize Single Logout subsystem for "
					+ "the authenticator " + getName(), e);
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
				SAMLSPProperties config = getSamlValidatorSettings();
				return config.getTrustChecker();
			}

			@Override
			public Collection<SAMLEndpointDefinition> getSLOEndpoints(NameIDType samlId)
			{
				SAMLSPProperties config = getSamlValidatorSettings();
				String configKey = config.getIdPConfigKey(samlId);
				if (configKey == null)
					return null;
				return config.getLogoutEndpointsFromStructuredList(configKey);
			}

			@Override
			public List<PublicKey> getTrustedKeys(NameIDType samlEntity)
			{
				SAMLSPProperties config = getSamlValidatorSettings();
				return config.getPublicKeysOfIdp(samlEntity.getStringValue());
			}
		};
		
		String sloPath = samlProperties.getValue(SAMLSPProperties.SLO_PATH);
		String sloRealm = samlProperties.getValue(SAMLSPProperties.SLO_REALM);
		
		if (sloPath == null || sloRealm == null)
		{
			log.debug("Single Logout functionality will be disabled for SAML authenticator "
					+ getName() + " as its path and/or realm are/is undefined.");
			return;
		}
		
		String samlId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		X509Credential credential = samlProperties.getRequesterCredential();
		IdentityTypeMapper idMapper = new IdentityTypeMapper(samlProperties);
		sloManager.deployAsyncServlet(sloPath,  
				idMapper, 
				600000, 
				samlId, 
				credential, 
				samlTrustProvider, 
				sloRealm);
		sloManager.deploySyncServlet(sloPath, 
				idMapper, 
				600000, 
				samlId, 
				credential, 
				samlTrustProvider, 
				sloRealm);
		
		sloReplyInstaller.enable();
	}
	
	@Override
	public RemoteAuthnContext createSAMLRequest(String idpConfigKey, String servletPath, 
			AuthenticationStepContext authnStepContext,
			LoginMachineDetails initialLoginMachine, 
			String ultimateReturnURL,
			AuthenticationTriggeringContext triggeringContext)
	{
		RemoteAuthnState baseState = new RemoteAuthnState(authnStepContext, this::processResponse, 
				initialLoginMachine, ultimateReturnURL, triggeringContext);
		RemoteAuthnContext context = new RemoteAuthnContext(getSamlValidatorSettings(), idpConfigKey, 
				baseState);
		
		SAMLSPProperties samlPropertiesCopy = context.getContextConfig();
		if (!samlPropertiesCopy.isIdPDefinitionComplete(idpConfigKey))
			throw new IllegalStateException("The selected IdP is not valid anymore, seems it was disabled");
		boolean sign = samlPropertiesCopy.isSignRequest(idpConfigKey);
		String requesterId = samlPropertiesCopy.getValue(SAMLSPProperties.REQUESTER_ID);
		String identityProviderURL = samlPropertiesCopy.getValue(idpConfigKey + SAMLSPProperties.IDP_ADDRESS);
		String requestedNameFormat = samlPropertiesCopy.getRequestedNameFormat(idpConfigKey);
		X509Credential credential = sign ? samlPropertiesCopy.getRequesterCredential() : null;
		
		AuthnRequestDocument request = SAMLHelper.createSAMLRequest(responseConsumerAddress, sign, 
				requesterId, identityProviderURL,
				requestedNameFormat, true, credential);
		context.setRequest(request.xmlText(), request.getAuthnRequest().getID(), servletPath);
		return context;
	}

	private AuthenticationResult processResponse(RemoteAuthnState remoteAuthnState)
	{
		RemoteAuthnContext castedState = (RemoteAuthnContext) remoteAuthnState;
		try
		{
			return verifySAMLResponse(castedState);
		} catch (Exception e)
		{
			log.error("Runtime error during SAML response processing or principal mapping", e);
			return RemoteAuthenticationResult.failed(null, e,
					new ResolvableError("WebSAMLRetrieval.authnFailedError"));
		}
	}
	
	private AuthenticationResult verifySAMLResponse(RemoteAuthnContext context)
	{
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(context);
			SAMLSPProperties config = context.getContextConfig();
			String idpKey = context.getContextIdpKey();
		
			TranslationProfile profile = getTranslationProfile(
					config, idpKey + CommonWebAuthnProperties.TRANSLATION_PROFILE,
					idpKey + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE);
			
			
			return getResult(input, profile, 
					context.getAuthenticationTriggeringContext().isSandboxTriggered(), 
					context.getRegistrationFormForUnknown(),
					context.isEnableAssociation());
		} catch (RemoteAuthenticationException e)
		{
			log.info("SAML response verification or processing failed", e);
			return RemoteAuthenticationResult.failed(e.getResult().getRemotelyAuthenticatedPrincipal(), e,
					new ResolvableError("WebSAMLRetrieval.authnFailedError"));
		}
	}
	
	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(RemoteAuthnContext context) 
			throws RemoteAuthenticationException 
	{
		ResponseDocument responseDocument;
		try
		{
			responseDocument = ResponseDocument.Factory.parse(context.getResponse());
		} catch (XmlException e)
		{
			throw new RemoteAuthenticationException("The SAML response can not be parsed - " +
					"XML data is corrupted", e);
		}
		
		SAMLResponseValidatorUtil responseValidatorUtil = new SAMLResponseValidatorUtil(
				getSamlValidatorSettings(), 
				replayAttackChecker, responseConsumerAddress);

		RemotelyAuthenticatedInput input = responseValidatorUtil.verifySAMLResponse(responseDocument, 
				context.getVerifiableResponse(),
				context.getRequestId(), 
				SAMLBindings.valueOf(context.getResponseBinding().toString()), 
				context.getGroupAttribute(), context.getContextIdpKey());
		return input;
	}
	
	@Override
	public SAMLSPProperties getSamlValidatorSettings()
	{
		return (SAMLSPProperties) myMetadataManager.getVirtualConfiguration();
	}
	
	@Override
	public IdPVisalSettings getVisualSettings(String configKey, Locale locale)
	{
		return myMetadataManager.getVisualSettings(configKey, locale);
	}
	
	@Override
	public VerificatorType getType()
	{
		return VerificatorType.Remote;
	}
	
	@Component
	public static class Factory extends AbstractCredentialVerificatorFactory
	{
		private MultiMetadataServlet metadataServlet;
		private Map<String, RemoteMetaManager> remoteMetadataManagers;
		private Map<String, LocalSPMetadataManager> localSPMetadataManagers;
		
		@Autowired
		public Factory(ObjectFactory<SAMLVerificator> factory, SamlContextManagement contextManagement,
				SharedEndpointManagement sharedEndpointManagement,
				SharedRemoteAuthenticationContextStore sharedRemoteAuthenticationContextStore) throws EngineException
		{
			super(NAME, DESC, factory);
			
			ServletHolder servlet = new ServletHolder(new SAMLResponseConsumerServlet(
					contextManagement, sharedRemoteAuthenticationContextStore));
			sharedEndpointManagement.deployInternalEndpointServlet(
					SAMLResponseConsumerServlet.PATH, servlet, false);
			
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


