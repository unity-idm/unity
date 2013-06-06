/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.saml;

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
		
		NameIDType issuer = aReq.getIssuer();
		String requestedFormat = getRequestedFormat(aReq);
		
		String issuerFormat = issuer.getFormat();
		if (issuerFormat == null)
			issuerFormat = SAMLConstants.NFORMAT_ENTITY;
		if (requestedFormat == null)
			requestedFormat = SAMLConstants.NFORMAT_ENTITY;
		if (!requestedFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			throw new SAMLRequesterException(
					SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"Requested identity type must be set to X.500 for ETD creation query");
		}
		
		if (!issuerFormat.equals(SAMLConstants.NFORMAT_DN))
		{
			throw new SAMLRequesterException(
					SAMLConstants.SubStatus.STATUS2_REQUEST_UNSUPP,
					"Query identity type must be set to X.500 for ETD creation query");
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
