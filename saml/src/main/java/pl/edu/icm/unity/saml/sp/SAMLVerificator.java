/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
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
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Binding irrelevant SAML logic: creation of a SAML authentication request and verification of the answer.
 * @author K. Benedyczak
 */
public class SAMLVerificator extends AbstractRemoteVerificator implements SAMLExchange
{
	private SAMLSPProperties samlProperties;
	private PKIManagement pkiMan;
	private MultiMetadataServlet metadataServlet;
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	private SAMLResponseValidatorUtil responseValidatorUtil;
	
	public SAMLVerificator(String name, String description, TranslationProfileManagement profileManagement, 
			AttributesManagement attrMan, PKIManagement pkiMan, ReplayAttackChecker replayAttackChecker,
			ExecutorsService executorsService, MultiMetadataServlet metadataServlet,
			URL baseAddress, String baseContext)
	{
		super(name, description, SAMLExchange.ID, profileManagement, attrMan);
		this.pkiMan = pkiMan;
		this.metadataServlet = metadataServlet;
		this.executorsService = executorsService;
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
		this.responseValidatorUtil = new SAMLResponseValidatorUtil(samlProperties, 
				replayAttackChecker, baseContext);
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
	public AuthnRequestDocument createSAMLRequest(String idpKey) throws InternalException
	{
		boolean sign = samlProperties.isSignRequest(idpKey);
		String requesterId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		String identityProviderURL = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
		String requestedNameFormat = samlProperties.getRequestedNameFormat(idpKey);
		X509Credential credential = sign ? samlProperties.getRequesterCredential() : null;
		return SAMLHelper.createSAMLRequest(responseConsumerAddress, sign, requesterId, identityProviderURL,
				requestedNameFormat, credential);
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
		
		
		RemotelyAuthenticatedInput input = responseValidatorUtil.verifySAMLResponse(responseDocument, 
				context.getRequestId(), 
				SAMLBindings.valueOf(context.getResponseBinding().toString()), 
				context.getGroupAttribute());
		return getResult(input, context.getTranslationProfile());
	}
	
	@Override
	public SAMLSPProperties getSamlValidatorSettings()
	{
		return samlProperties;
	}
}










