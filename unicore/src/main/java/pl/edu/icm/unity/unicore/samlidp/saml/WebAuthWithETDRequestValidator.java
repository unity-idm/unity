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
import pl.edu.icm.unity.saml.validator.WebAuthRequestValidator;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.time.Duration;

/**
 * Extension of the {@link WebAuthRequestValidator}. Requests for ETD generation
 * are required to have X.500 issuer and required user's identity type must be as well X.500.
 * <p>
 * This class is a twin of {@link SoapAuthWithETDRequestValidator}
 * 
 * @author K. Benedyczak
 */
public class WebAuthWithETDRequestValidator extends WebAuthRequestValidator
{

	public WebAuthWithETDRequestValidator(String consumerEndpointUri,
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
		NameIDType issuer = authnRequest.getIssuer();
		checkX500Issuer(issuer);
	}
	
	
	/**
	 * Checks if the given name id is of X.500 type.
	 * @param issuer
	 * @throws SAMLRequesterException
	 */
	protected void checkX500Issuer(NameIDType issuer) throws SAMLRequesterException
	{
		if (issuer == null)
			throw new SAMLRequesterException("Issuer of SAML request must be present in SSO AuthN");
		if (issuer.getFormat() == null || !issuer.getFormat().equals(SAMLConstants.NFORMAT_DN))
			throw new SAMLRequesterException(
					SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"Query identity type must be set to X.500 for ETD creation query");
		if (issuer.getStringValue() == null)
			throw new SAMLRequesterException("Issuer value of SAML request must be present in SSO AuthN");
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
