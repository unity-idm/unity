/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.saml;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.messages.SAMLVerifiableElement;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.saml.validator.UnityAuthnRequestValidator;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.time.Duration;

/**
 * Extension of the {@link UnityAuthnRequestValidator}. Requests for ETD generation
 * can have X.500 issuer as well as entity. Requested format must be X.500.
 * <p> 
 * This class is a twin of {@link WebAuthWithETDRequestValidator}, but it doesn't require X.500 issuer:
 * if the issuer is of entity type, then it is also OK, and the wrapping endpoint should fallback to the standard
 * processing of SAML authn (UNICORE unaware).
 * 
 * @author K. Benedyczak
 */
public class SoapAuthWithETDRequestValidator extends UnityAuthnRequestValidator
{

	public SoapAuthWithETDRequestValidator(String consumerEndpointUri,
			SamlTrustChecker trustChecker, Duration requestValidity,
			ReplayAttackChecker replayChecker)
	{
		super(consumerEndpointUri, trustChecker, requestValidity, replayChecker);
	}

	@Override
	public void validate(AuthnRequestDocument authenticationRequestDoc, SAMLVerifiableElement verifiableMessage) throws SAMLServerException
	{
		AuthnRequestType aReq = authenticationRequestDoc.getAuthnRequest();
		super.validate(authenticationRequestDoc, verifiableMessage);
		
		String requestedFormat = getRequestedFormat(aReq);
		if (!requestedFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			throw new SAMLRequesterException(
					SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"Requested identity type must be set to X.500 for ETD creation query");
		}
	}
	
	@Override
	protected void validateIssuer(AuthnRequestType authnRequest) throws SAMLServerException
	{
		checkX500Issuer(authnRequest.getIssuer());
	}
	
	/**
	 * Checks if the given name id is of X.500 type or entity type.
	 * @param issuer
	 * @throws SAMLRequesterException
	 */
	private void checkX500Issuer(NameIDType issuer) throws SAMLRequesterException
	{
		if (issuer == null)
			throw new SAMLRequesterException("Issuer of SAML request must be present in SSO AuthN");
		if (issuer.getFormat() != null && !issuer.getFormat().equals(SAMLConstants.NFORMAT_DN)
				&& !issuer.getFormat().equals(SAMLConstants.NFORMAT_ENTITY))
			throw new SAMLRequesterException(
					SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"Query identity type must be set to X.500 or entity for SAML request");
		if (issuer.getStringValue() == null)
			throw new SAMLRequesterException("Issuer value of SAML request must be present in SSO AuthN");
		if (issuer.getFormat() != null && issuer.getFormat().equals(SAMLConstants.NFORMAT_DN))
		{
			try
			{
				X500NameUtils.getX500Principal(issuer.getStringValue());
			} catch (Exception e)
			{
				throw new SAMLRequesterException("Issuer value of SAML request is not a valid X.500 name: " 
						+ e.getMessage());
			}
		}
	}
}
