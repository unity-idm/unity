/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.xmlbeans.XmlException;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.AssertionValidator;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnResponseValidator;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.SAMLHelper;
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
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
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
	private ReplayAttackChecker replayAttackChecker;
	private MultiMetadataServlet metadataServlet;
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	
	public SAMLVerificator(String name, String description, TranslationProfileManagement profileManagement, 
			AttributesManagement attrMan, PKIManagement pkiMan, ReplayAttackChecker replayAttackChecker,
			ExecutorsService executorsService, MultiMetadataServlet metadataServlet,
			URL baseAddress, String baseContext)
	{
		super(name, description, SAMLExchange.ID, profileManagement, attrMan);
		this.pkiMan = pkiMan;
		this.replayAttackChecker = replayAttackChecker;
		this.metadataServlet = metadataServlet;
		this.executorsService = executorsService;
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
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
		boolean sign = samlProperties.getBooleanValue(idpKey + SAMLSPProperties.IDP_SIGN_REQUEST);
		String requesterId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		String identityProviderURL = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
		String requestedNameFormat = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_REQUESTED_NAME_FORMAT);
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
		
		String consumerSamlName = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		
		SamlTrustChecker samlTrustChecker;
		try
		{
			samlTrustChecker = samlProperties.getTrustChecker();
		} catch (ConfigurationException e1)
		{
			throw new AuthenticationException("The SAML response can not be verified - " +
					"there is an internal configuration error", e1);
		}
		
		SSOAuthnResponseValidator validator = new SSOAuthnResponseValidator(
				consumerSamlName, responseConsumerAddress, 
				context.getRequestId(), AssertionValidator.DEFAULT_VALIDITY_GRACE_PERIOD, 
				samlTrustChecker, replayAttackChecker, 
				SAMLBindings.valueOf(context.getResponseBinding().toString()));
		
		try
		{
			validator.validate(responseDocument);
		} catch (SAMLValidationException e)
		{
			throw new AuthenticationException("The SAML response is either invalid or is issued " +
					"by an untrusted identity provider.", e);
		}

		RemotelyAuthenticatedInput input = convertAssertion(responseDocument, validator, 
				context.getGroupAttribute());

		return getResult(input, context.getTranslationProfile());
	}
	
	private RemotelyAuthenticatedInput convertAssertion(ResponseDocument responseDocument,
			SSOAuthnResponseValidator validator, String groupA) throws AuthenticationException
	{
		NameIDType issuer = responseDocument.getResponse().getIssuer();
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(issuer.getStringValue());
		
		input.setIdentities(getAuthenticatedIdentities(validator));
		List<RemoteAttribute> remoteAttributes = getAttributes(validator);
		input.setAttributes(remoteAttributes);
		input.setGroups(getGroups(remoteAttributes, groupA));
		
		return input;
	}
	
	private List<RemoteIdentity> getAuthenticatedIdentities(SSOAuthnResponseValidator validator)
	{
		List<AssertionDocument> authnAssertions = validator.getAuthNAssertions();
		List<RemoteIdentity> ret = new ArrayList<>(authnAssertions.size());
		for (int i=0; i<authnAssertions.size(); i++)
		{
			NameIDType samlName = authnAssertions.get(i).getAssertion().getSubject().getNameID();
			ret.add(new RemoteIdentity(samlName.getStringValue(), samlName.getFormat()));
		}
		return ret;
	}
	
	private List<RemoteAttribute> getAttributes(SSOAuthnResponseValidator validator) throws AuthenticationException
	{
		List<AssertionDocument> assertions = validator.getOtherAssertions();
		List<RemoteAttribute> ret = new ArrayList<>(assertions.size());
		for (AssertionDocument assertion: assertions)
		{
			AttributeAssertionParser parser = new AttributeAssertionParser(assertion);
			List<ParsedAttribute> parsedAttrs;
			try
			{
				parsedAttrs = parser.getAttributes();
			} catch (SAMLValidationException e)
			{
				throw new AuthenticationException("Problem retrieving attributes from the SAML data", e);
			}
			for (ParsedAttribute pa: parsedAttrs)
			{
				List<String> strValues = pa.getStringValues();
				ret.add(new RemoteAttribute(pa.getName(), 
						(Object[]) strValues.toArray(new String[strValues.size()])));
			}
		}
		return ret;
	}
	
	private List<RemoteGroupMembership> getGroups(List<RemoteAttribute> remoteAttributes, String groupA)
	{
		List<RemoteGroupMembership> ret = new ArrayList<>();
		if (groupA == null)
			return ret;
		for (RemoteAttribute ra: remoteAttributes)
		{
			if (ra.getName().equals(groupA))
			{
				for (Object value: ra.getValues())
				{
					ret.add(new RemoteGroupMembership((String) value));
				}
			}
		}
		return ret;
	}
	
	@Override
	public SAMLSPProperties getSamlValidatorSettings()
	{
		return samlProperties;
	}
}










