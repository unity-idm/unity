/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.assertion.AssertionParser;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.AssertionValidator;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnResponseValidator;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.AssertionType;
import xmlbeans.org.oasis.saml2.assertion.AuthnContextType;
import xmlbeans.org.oasis.saml2.assertion.AuthnStatementType;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Class used to validate SAML responses. Used by ECP and SP subsystems.
 * The code validates the SAML response and performs initial parsing, so the output can be directly
 * feed to translation profile engine.
 *  
 * @author K. Benedyczak
 */
public class SAMLResponseValidatorUtil
{
	public static final String AUTHN_CONTEXT_CLASS_REF_ATTR = "authnContextClassRef";
	private SAMLSPProperties samlProperties;
	private ReplayAttackChecker replayAttackChecker;
	private String responseConsumerAddress;
	
	public SAMLResponseValidatorUtil(SAMLSPProperties samlProperties,
			ReplayAttackChecker replayAttackChecker, String responseConsumerAddress)
	{
		super();
		this.samlProperties = samlProperties;
		this.replayAttackChecker = replayAttackChecker;
		this.responseConsumerAddress = responseConsumerAddress;
	}


	public RemotelyAuthenticatedInput verifySAMLResponse(ResponseDocument responseDocument,
			String requestId, SAMLBindings binding, String groupAttribute, String configKey) 
					throws AuthenticationException
	{
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
		
		X509Credential credential = samlProperties.getRequesterCredential();
		PrivateKey decryptKey = credential == null ? null : credential.getKey();
		SSOAuthnResponseValidator validator = new SSOAuthnResponseValidator(
				consumerSamlName, responseConsumerAddress, 
				requestId, AssertionValidator.DEFAULT_VALIDITY_GRACE_PERIOD, 
				samlTrustChecker, replayAttackChecker, binding, 
				decryptKey);
		try
		{
			validator.validate(responseDocument);
		} catch (SAMLValidationException e)
		{
			throw new AuthenticationException("The SAML response is either invalid or is issued " +
					"by an untrusted identity provider.", e);
		}

		return convertAssertion(responseDocument, validator, groupAttribute, configKey);
	}
	
	
	RemotelyAuthenticatedInput convertAssertion(ResponseDocument responseDocument,
			SSOAuthnResponseValidator validator, String groupA, String configKey) throws AuthenticationException
	{
		xmlbeans.org.oasis.saml2.protocol.ResponseType resp = responseDocument.getResponse();
		NameIDType issuer = resp.getIssuer();
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput(issuer.getStringValue());
		
		input.setIdentities(getAuthenticatedIdentities(validator));
		List<RemoteAttribute> remoteAttributes = getAttributes(validator);
		remoteAttributes.add(getAuthnContextClassAttribute(validator));
		input.setAttributes(remoteAttributes);
		input.setRawAttributes(input.getAttributes());
		input.setGroups(getGroups(remoteAttributes, groupA));

		addSessionParticipants(validator, issuer, input, configKey);
		
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

	private void addSessionParticipants(SSOAuthnResponseValidator validator,
			NameIDType issuer, RemotelyAuthenticatedInput input, String configKey)
	{
		List<SAMLEndpointDefinition> logoutEndpoints = samlProperties.
				getLogoutEndpointsFromStructuredList(configKey);
		String localSPSamlId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		String localCredential = samlProperties.getValue(SAMLSPProperties.CREDENTIAL);
		Set<String> validCerts = samlProperties.getCertificateNames(configKey);
		List<AssertionDocument> authnAssertions = validator.getAuthNAssertions();
		for (int i=0; i<authnAssertions.size(); i++)
		{
			AssertionType authNAss = authnAssertions.get(i).getAssertion();
			String sessionIndex = null;
			for (AuthnStatementType authNStatement: authNAss.getAuthnStatementArray())
			{
				sessionIndex = authNStatement.getSessionIndex();
				if (sessionIndex != null)
				{
					SAMLSessionParticipant participant = new SAMLSessionParticipant(
							issuer.getStringValue(), 
							authNAss.getSubject().getNameID(), sessionIndex,
							logoutEndpoints, localSPSamlId, 
							localCredential, validCerts);
					input.addSessionParticipant(participant);
				}
			}
		}
	}
	
	private List<RemoteAttribute> getAttributes(SSOAuthnResponseValidator validator) throws AuthenticationException
	{
		List<AssertionDocument> assertions = validator.getAttributeAssertions();
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

	private RemoteAttribute getAuthnContextClassAttribute(SSOAuthnResponseValidator validator)
	{
		List<AssertionDocument> assertions = validator.getAuthNAssertions();
		List<String> values = new ArrayList<>();
		for (AssertionDocument assertion: assertions)
		{
			AssertionParser parser = new AssertionParser(assertion);
			AuthnStatementType[] authnStatements = parser.getXMLBean().getAuthnStatementArray();
			for (AuthnStatementType statement: authnStatements)
			{
				AuthnContextType authnContext = statement.getAuthnContext();
				if (authnContext != null)
				{
					String authnContextClassRef = authnContext.getAuthnContextClassRef();
					if (authnContextClassRef != null)
						values.add(authnContextClassRef);
				}
			}
		}
		return new RemoteAttribute(AUTHN_CONTEXT_CLASS_REF_ATTR,
				(Object[]) values.toArray(new String[values.size()]));
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

}
