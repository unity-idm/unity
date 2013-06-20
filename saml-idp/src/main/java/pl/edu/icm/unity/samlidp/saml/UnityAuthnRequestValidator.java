/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnRequestValidator;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

/**
 * Validates SAML Authentication Request. Extends {@link SSOAuthnRequestValidator}
 * with additional constraints as Unity doesn't support everything what is expressible in SAML 
 * authn requests. Implemented restrictions (or limitations) are:
 * <ul>
 * <li> subject must not be set
 * <li> requestedAuthnContext must not be set
 * <li> assertionConsumerServiceIndex must not be set
 * <li> AttributeconsumingServiceIndex must not be set
 * <li> AttributeConsumingServiceURL must be set. This is because it is the only mean currently to set 
 * the mandatory receiver when producing an answer.
 * </ul>
 * This class is binding transparent, it can be used as a base for binding specific validators.
 * 
 * @author K. Benedyczak
 */
public class UnityAuthnRequestValidator extends SSOAuthnRequestValidator
{
	public UnityAuthnRequestValidator(String consumerEndpointUri, SamlTrustChecker trustChecker,
			long requestValidity, ReplayAttackChecker replayChecker)
	{
		super(consumerEndpointUri, trustChecker, requestValidity, replayChecker);
	}

	public void validate(AuthnRequestDocument authenticationRequestDoc) throws SAMLServerException
	{
		AuthnRequestType req = authenticationRequestDoc.getAuthnRequest();
		super.validate(authenticationRequestDoc, req);
		
		//1 - presence of Subject element in request
		//WARNING - if this is implemented then filtering of inactive identities
		//must be also applied in this class.
		if (req.getSubject() != null)
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"This implementation doesn't support authn " +
					"requests with Subject set.");
		//2 - requestedAuthnContext
		if (req.getRequestedAuthnContext() != null)
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"This implementation doesn't support authn " +
					"requests with RequestedAuthnContext set.");
		//3 - assertionConsumerServiceIndex
		if (req.isSetAssertionConsumerServiceIndex())
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"This implementation doesn't support authn " +
					"requests with AssertionConsumerServiceIndex set.");
		//4 - AttributeconsumingServiceIndex
		if (req.isSetAttributeConsumingServiceIndex())
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"This implementation doesn't support authn " +
					"requests with AttributeConsumingServiceIndex set.");
		//5 - AttributeConsumingServiceURL mandatory
		if (!req.isSetAssertionConsumerServiceURL())
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"This implementation doesn't support authn " +
					"requests without AttributeConsumingServiceURL.");
	}
}
