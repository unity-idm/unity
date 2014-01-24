/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.proto.AuthnRequest;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.AssertionValidator;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnResponseValidator;
import eu.unicore.security.AuthenticationException;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.NameFormat;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.AttributeStatementType;
import xmlbeans.org.oasis.saml2.assertion.AttributeType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Binding irrelevant SAML logic: creation of a SAML authentication request and verification of the answer.
 * @author K. Benedyczak
 */
public class SAMLValidator extends AbstractRemoteVerificator implements SAMLExchange
{
	private SAMLSPProperties samlProperties;
	private PKIManagement pkiMan;
	private ReplayAttackChecker replayAttackChecker;
	
	public SAMLValidator(String name, String description, TranslationProfileManagement profileManagement, 
			AttributesManagement attrMan, PKIManagement pkiMan, ReplayAttackChecker replayAttackChecker)
	{
		super(name, description, SAMLExchange.ID, profileManagement, attrMan);
		this.pkiMan = pkiMan;
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

	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			samlProperties = new SAMLSPProperties(properties, pkiMan);
			setTranslationProfile(samlProperties.getValue(SAMLSPProperties.TRANSLATION_PROFILE));
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator(?)", e);
		} catch (EngineException e)
		{
			throw new InternalException("Problem with the translation profile of the SAML verificator", e);
		}
	}

	@Override
	public AuthnRequestDocument createSAMLRequest(String identityProviderURL, String returnURL, boolean sign) 
			throws InternalException
	{
		String requestrId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		NameID issuer = new NameID(requestrId, SAMLConstants.NFORMAT_ENTITY);
		AuthnRequest request = new AuthnRequest(issuer.getXBean());
		
		NameFormat requestedFormat = samlProperties.getEnumValue(SAMLSPProperties.REQUESTED_NAME_FORMAT, 
				NameFormat.class);
		if (requestedFormat != null)
			request.setFormat(requestedFormat.getSamlRepresentation());
		request.getXMLBean().setDestination(identityProviderURL);
		request.getXMLBean().setAssertionConsumerServiceURL(returnURL);

		if (sign)
		{
			try
			{
				X509Credential credential = pkiMan.getCredential(
						samlProperties.getValue(SAMLSPProperties.CREDENTIAL));
				request.sign(credential.getKey(), credential.getCertificateChain());
			} catch (Exception e)
			{
				throw new InternalException("Can't sign request", e);
			}
		}
		return request.getXMLBeanDoc();
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
				consumerSamlName, context.getSpUrl(), 
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

		RemotelyAuthenticatedInput input = convertAssertion(responseDocument, validator);
		try
		{
			return getResult(input);
		} catch (EngineException e)
		{
			throw new AuthenticationException("Problem retrieving the contents of the (proper) SAML data", e);
		}
	}
	
	private RemotelyAuthenticatedInput convertAssertion(ResponseDocument responseDocument,
			SSOAuthnResponseValidator validator)
	{
		NameIDType issuer = responseDocument.getResponse().getIssuer();
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(issuer.getStringValue());
		
		input.setIdentities(getAuthenticatedIdentities(validator));
		List<RemoteAttribute> remoteAttributes = getAttributes(validator);
		input.setAttributes(remoteAttributes);
		input.setGroups(getGroups(remoteAttributes));
		
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
	
	private List<RemoteAttribute> getAttributes(SSOAuthnResponseValidator validator)
	{
		List<AssertionDocument> assertions = validator.getOtherAssertions();
		List<RemoteAttribute> ret = new ArrayList<>(assertions.size());
		for (AssertionDocument assertion: assertions)
		{
			AttributeStatementType[] attrStatements = assertion.getAssertion().getAttributeStatementArray();
			if (attrStatements == null)
				continue;
			for (AttributeStatementType attrStatement: attrStatements)
			{
				AttributeType[] attributes = attrStatement.getAttributeArray();
				for (AttributeType attribute: attributes)
				{
					XmlObject[] attrValues = attribute.getAttributeValueArray();
					Object[] attrValuesString = convertValues(attrValues);
					ret.add(new RemoteAttribute(attribute.getName(), attrValuesString));
				}
			}
		}
		return ret;
	}
	
	private String[] convertValues(XmlObject[] attrValues)
	{
		String[] ret = new String[attrValues.length];
		for (int i=0; i<attrValues.length; i++)
		{
			ret[i] = attrValues[i].xmlText();
		}
		return ret;
	}

	private List<RemoteGroupMembership> getGroups(List<RemoteAttribute> remoteAttributes)
	{
		String groupA = samlProperties.getValue(SAMLSPProperties.GROUP_MEMBERSHIP_ATTRIBUTE);
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










