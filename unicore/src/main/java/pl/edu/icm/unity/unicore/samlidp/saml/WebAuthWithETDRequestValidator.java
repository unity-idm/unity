/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.saml;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.samlidp.web.WebAuthRequestValidator;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;
import xmlbeans.org.oasis.saml2.protocol.NameIDPolicyType;

/**
 * Extension of the {@link WebAuthRequestValidator}. Requests for ETD generation
 * are required to have X.500 issuer and required user's identity type must be as well X.500.
 * 
 * @author K. Benedyczak
 */
public class WebAuthWithETDRequestValidator extends WebAuthRequestValidator
{

	public WebAuthWithETDRequestValidator(String consumerEndpointUri,
			SamlTrustChecker trustChecker, long requestValidity,
			ReplayAttackChecker replayChecker)
	{
		super(consumerEndpointUri, trustChecker, requestValidity, replayChecker);
	}

	@Override
	public void validate(AuthnRequestDocument authenticationRequestDoc) throws SAMLServerException
	{
		AuthnRequestType aReq = authenticationRequestDoc.getAuthnRequest();
		super.validate(authenticationRequestDoc, aReq);
		
		String requestedFormat = getRequestedFormat(aReq);
		
		if (requestedFormat == null)
			requestedFormat = SAMLConstants.NFORMAT_ENTITY;
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
		if (issuer == null)
			throw new SAMLRequesterException("Issuer of SAML request must be present in SSO AuthN");
		if (issuer.getFormat() == null && !issuer.getFormat().equals(SAMLConstants.NFORMAT_DN))
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
	
	protected String getRequestedFormat(AuthnRequestType aReq)
	{
		String requestedFormat = null;
		NameIDPolicyType nameIDPolicy = aReq.getNameIDPolicy();
		if (nameIDPolicy != null)
			requestedFormat = nameIDPolicy.getFormat();
		if (requestedFormat == null)
			return SAMLConstants.NFORMAT_UNSPEC;
		return requestedFormat;
	}

}
