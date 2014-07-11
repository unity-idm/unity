/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.xmlbeans.XmlException;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.SAMLHelper;
import pl.edu.icm.unity.saml.SAMLResponseValidatorUtil;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Binding irrelevant SAML logic: creation of a SAML authentication request and verification of the answer.
 * @author K. Benedyczak
 */
public class SAMLVerificator extends AbstractRemoteVerificator implements SAMLExchange
{
	private UnityServerConfiguration mainConfig;
	private SAMLSPProperties samlProperties;
	private PKIManagement pkiMan;
	private MultiMetadataServlet metadataServlet;
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private RemoteMetaManager myMetadataManager;
	private ReplayAttackChecker replayAttackChecker;
	
	public SAMLVerificator(String name, String description, TranslationProfileManagement profileManagement, 
			InputTranslationEngine trEngine, PKIManagement pkiMan, ReplayAttackChecker replayAttackChecker,
			ExecutorsService executorsService, MultiMetadataServlet metadataServlet,
			URL baseAddress, String baseContext, Map<String, RemoteMetaManager> remoteMetadataManagers,
			UnityServerConfiguration mainConfig)
	{
		super(name, description, SAMLExchange.ID, profileManagement, trEngine);
		this.remoteMetadataManagers = remoteMetadataManagers;
		this.pkiMan = pkiMan;
		this.mainConfig = mainConfig;
		this.metadataServlet = metadataServlet;
		this.executorsService = executorsService;
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
		this.replayAttackChecker = replayAttackChecker;
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
	public void setSerializedConfiguration(String source) throws InternalException
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
		
		if (samlProperties.getBooleanValue(SamlProperties.PUBLISH_METADATA))
			exposeMetadata();
		if (!remoteMetadataManagers.containsKey(instanceName))
		{
			myMetadataManager = new RemoteMetaManager(samlProperties, 
					mainConfig, executorsService, pkiMan);
			remoteMetadataManagers.put(instanceName, myMetadataManager);
			myMetadataManager.start();
		} else
		{
			myMetadataManager = remoteMetadataManagers.get(instanceName);
			myMetadataManager.setBaseConfiguration(samlProperties);
		}
	}

	private void exposeMetadata()
	{
		String metaPath = samlProperties.getValue(SAMLSPProperties.METADATA_PATH);
		IndexedEndpointType consumerEndpoint = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint.setIndex(1);
		consumerEndpoint.setBinding(SAMLConstants.BINDING_HTTP_POST);
		consumerEndpoint.setLocation(responseConsumerAddress);
		consumerEndpoint.setIsDefault(true);

		IndexedEndpointType consumerEndpoint2 = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint2.setIndex(2);
		consumerEndpoint2.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);
		consumerEndpoint2.setLocation(responseConsumerAddress);
		consumerEndpoint2.setIsDefault(false);

		IndexedEndpointType[] assertionConsumerEndpoints = new IndexedEndpointType[] {consumerEndpoint,
				consumerEndpoint2};
		MetadataProvider provider = MetadataProviderFactory.newSPInstance(samlProperties, 
				executorsService, assertionConsumerEndpoints);
		metadataServlet.addProvider("/" + metaPath, provider);
	}
	
	@Override
	public RemoteAuthnContext createSAMLRequest(String idpKey, String servletPath) throws InternalException
	{
		RemoteAuthnContext context = new RemoteAuthnContext(getSamlValidatorSettings(), idpKey);
		
		SAMLSPProperties samlPropertiesCopy = context.getContextConfig();
		boolean sign = samlPropertiesCopy.isSignRequest(idpKey);
		String requesterId = samlPropertiesCopy.getValue(SAMLSPProperties.REQUESTER_ID);
		String identityProviderURL = samlPropertiesCopy.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
		String requestedNameFormat = samlPropertiesCopy.getRequestedNameFormat(idpKey);
		X509Credential credential = sign ? samlPropertiesCopy.getRequesterCredential() : null;
		
		AuthnRequestDocument request = SAMLHelper.createSAMLRequest(responseConsumerAddress, sign, 
				requesterId, identityProviderURL,
				requestedNameFormat, credential);
		context.setRequest(request.xmlText(), request.getAuthnRequest().getID(), servletPath);
		return context;
	}

	@Override
	public AuthenticationResult verifySAMLResponse(RemoteAuthnContext context) throws AuthenticationException
	{
		ResponseDocument responseDocument;
		try
		{
			responseDocument = ResponseDocument.Factory.parse(context.getResponse());
		} catch (XmlException e)
		{
			throw new AuthenticationException("The SAML response can not be parsed - " +
					"XML data is corrupted", e);
		}
		
		SAMLSPProperties config = context.getContextConfig();
		String idpKey = context.getContextIdpKey();
		SAMLResponseValidatorUtil responseValidatorUtil = new SAMLResponseValidatorUtil(
				getSamlValidatorSettings(), 
				replayAttackChecker, responseConsumerAddress);
		RemotelyAuthenticatedInput input = responseValidatorUtil.verifySAMLResponse(responseDocument, 
				context.getRequestId(), 
				SAMLBindings.valueOf(context.getResponseBinding().toString()), 
				context.getGroupAttribute());
		return getResult(input, config.getValue(idpKey + SAMLSPProperties.IDP_TRANSLATION_PROFILE));
	}
	
	@Override
	public SAMLSPProperties getSamlValidatorSettings()
	{
		return myMetadataManager.getVirtualConfiguration();
	}
}










