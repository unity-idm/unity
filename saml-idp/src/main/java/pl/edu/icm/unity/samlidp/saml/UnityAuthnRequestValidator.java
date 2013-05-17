/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.samlidp.saml;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnRequestValidator;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

/**
 * Validates SAML Authentication Request. Extends {@link SSOAuthnRequestValidator}
 * with additional constraints as UVOS doesn't support everything what is expressible in SAML 
 * authn requests. Implemented restrictions (or limitations) are:
 * <ul>
 * <li> consumer service URL must be always set.
 * <li> subject must not be set
 * <li> requestedAuthnContext must not be set
 * <li> assertionConsumerServiceIndex must not be set
 * <li> AttributeconsumingServiceIndex must not be set
 * </ul>
 * What is more this validator allows for DN type request issuers. This is not allowed by the SAML
 * profile, but we need this for authN requests resulting in issuance of ETD assertions.
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
		//5 - consumer service mandatory
		if (req.getAssertionConsumerServiceURL() == null)
			throw new SAMLResponderException(SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"This implementation requires that consumerServiceURL is in " +
					"SAML Authentication.");
	}
	
	@Override
	protected void validateIssuer(AuthnRequestType authnRequest) throws SAMLServerException
	{
		NameIDType issuer = authnRequest.getIssuer();
		if (issuer == null)
			throw new SAMLRequesterException("Issuer of SAML request must be present in SSO AuthN");
		if (issuer.getFormat() != null && !(issuer.getFormat().equals(SAMLConstants.NFORMAT_ENTITY) ||
				issuer.getFormat().equals(SAMLConstants.NFORMAT_DN)))
			throw new SAMLRequesterException("Issuer of SAML request must be of Entity or X509SubjectName " +
					"type in SSO AuthN queries to this service. It is: " + issuer.getFormat());
		if (issuer.getStringValue() == null)
			throw new SAMLRequesterException("Issuer value of SAML request must be present in SSO AuthN");
	}

}
