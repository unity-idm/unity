/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.validator;

import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;

/**
 * Adds Unity limitations to standard Web SSO validation:
 * only HTTP-POST responses and no support for passive authN.
 * @author K. Benedyczak
 */
public class WebAuthRequestValidator extends UnityAuthnRequestValidator
{

	public WebAuthRequestValidator(String consumerEndpointUri, SamlTrustChecker trustChecker,
			long requestValidity, ReplayAttackChecker replayChecker)
	{
		super(consumerEndpointUri, trustChecker, requestValidity, replayChecker);
	}

	@Override
	public void validate(AuthnRequestDocument authenticationRequestDoc) throws SAMLServerException
	{
		AuthnRequestType aReq = authenticationRequestDoc.getAuthnRequest();
		super.validate(authenticationRequestDoc, aReq);
		if (aReq.getProtocolBinding() != null && 
				!aReq.getProtocolBinding().equals(SAMLConstants.BINDING_HTTP_POST))
		{
			throw new SAMLResponderException("Received SAML request requiring " + aReq.getProtocolBinding()
					+ ". This is not supported, this implementation can only send responses with" +
					"HTTP-POST binding");
		}
		if (aReq.isSetIsPassive() && aReq.getIsPassive())
		{
			throw new SAMLResponderException("Received a SAML request requiring a " +
					"passive authentication, but it is unsupported.");
		}
		if (!aReq.isSetAssertionConsumerServiceURL() && !knownRequesters.contains(
				aReq.getIssuer().getStringValue()))
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"Received a SAML request without AssertionConsumingServiceURL " +
					"and the requester's response endpoint is not configured.");
	}
}
