/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.validator;

import java.util.HashSet;
import java.util.Set;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnRequestValidator;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;
import xmlbeans.org.oasis.saml2.protocol.NameIDPolicyType;

/**
 * Validates SAML Authentication Request. Extends {@link SSOAuthnRequestValidator}
 * with additional constraints as Unity doesn't support everything what is expressible in SAML 
 * authn requests. Implemented restrictions (or limitations) are:
 * <ul>
 * <li> subject must not be set
 * <li> requestedAuthnContext must not be set
 * <li> AssertionConsumerServiceIndex must not be set
 * <li> AttributeConsumingServiceIndex must not be set
 * <li> AssertionConsumingServiceURL must be set if it is not configured.
 * </ul>
 * This class is binding transparent, it can be used as a base for binding specific validators.
 * 
 * @author K. Benedyczak
 */
public class UnityAuthnRequestValidator extends SSOAuthnRequestValidator
{
	protected Set<String> knownRequesters;
	
	public UnityAuthnRequestValidator(String consumerEndpointUri, SamlTrustChecker trustChecker,
			long requestValidity, ReplayAttackChecker replayChecker)
	{
		super(consumerEndpointUri, trustChecker, requestValidity, replayChecker);
		knownRequesters = new HashSet<>();
	}

	/**
	 * Adds a new known requester, for which we have a response URL defined out of bands.
	 * @param requesterName
	 */
	public void addKnownRequester(String requesterName)
	{
		knownRequesters.add(requesterName);
	}
	
	@Override
	public void validate(AuthnRequestDocument authenticationRequestDoc) throws SAMLServerException
	{
		AuthnRequestType req = authenticationRequestDoc.getAuthnRequest();
		super.validate(authenticationRequestDoc);
		
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
		//4 - AttributeConsumingServiceIndex
		if (req.isSetAttributeConsumingServiceIndex())
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"This implementation doesn't support authn " +
					"requests with AttributeConsumingServiceIndex set.");
		//5 - AssertionConsumingServiceURL mandatory if we don't know the requester
		if (!req.isSetAssertionConsumerServiceURL() && !knownRequesters.contains(
				req.getIssuer().getStringValue()))
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"AssertionConsumingServiceURL is not set and the requester's " +
					"response endpoint is not configured.");
	}
	

	/**
	 * Useful for subclasses
	 * @param aReq
	 * @return
	 */
	protected String getRequestedFormat(AuthnRequestType aReq)
	{
		String requestedFormat = null;
		NameIDPolicyType nameIDPolicy = aReq.getNameIDPolicy();
		if (nameIDPolicy != null)
			requestedFormat = nameIDPolicy.getFormat();
		if (requestedFormat == null)
			return SAMLConstants.NFORMAT_UNSPEC;
		if (requestedFormat.equals(SAMLConstants.NFORMAT_UNSPEC))
			requestedFormat = SAMLConstants.NFORMAT_ENTITY;
		return requestedFormat;
	}
}
