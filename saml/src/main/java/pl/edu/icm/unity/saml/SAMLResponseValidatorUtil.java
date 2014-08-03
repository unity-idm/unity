/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.AssertionValidator;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnResponseValidator;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Class used to validate SAML responses. Used by ECP and SP subsystems.
 * The code validates the SAML response and performs initial parsing, so the output can be directly
 * feed to translation profile engine.
 *  
 * @author K. Benedyczak
 */
public class SAMLResponseValidatorUtil
{
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
			String requestId, SAMLBindings binding, String groupAttribute) throws AuthenticationException
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
		
		SSOAuthnResponseValidator validator = new SSOAuthnResponseValidator(
				consumerSamlName, responseConsumerAddress, 
				requestId, AssertionValidator.DEFAULT_VALIDITY_GRACE_PERIOD, 
				samlTrustChecker, replayAttackChecker, binding, 
				samlProperties.getRequesterCredential().getKey());
		
		try
		{
			validator.validate(responseDocument);
		} catch (SAMLValidationException e)
		{
			throw new AuthenticationException("The SAML response is either invalid or is issued " +
					"by an untrusted identity provider.", e);
		}

		return convertAssertion(responseDocument, validator, groupAttribute);
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

}
