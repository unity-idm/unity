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
import pl.edu.icm.unity.samlidp.saml.WebAuthRequestValidator;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

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
		SoapAuthWithETDRequestValidator.checkX500Issuer(issuer);
	}
}
