/*
 * Copyright (c) 2012 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.samly2.messages.SAMLVerifiableElement;
import eu.unicore.samly2.trust.CheckingMode;
import eu.unicore.samly2.trust.ResponseTrustCheckResult;
import eu.unicore.samly2.trust.SamlTrustChecker;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.protocol.RequestAbstractType;
import xmlbeans.org.oasis.saml2.protocol.StatusResponseType;

/**
 * Configures and performs checking whether consumer trusts the issuer of 
 * SAML assertion, request or response.
 * <p>
 * This implementation is always trusting everybody. Effectively it is useful on
 * server side, when everybody is allowed to send SAML requests or when authorization
 * is performed with other, non-SAML related methods. 
 * @author K. Benedyczak
 */
public class TrustAllTrustChecker implements SamlTrustChecker
{
	@Override
	public void checkTrust(SAMLVerifiableElement message, RequestAbstractType request) throws SAMLValidationException
	{
	}

	@Override
	public void checkTrust(AssertionDocument assertionDoc,
			ResponseTrustCheckResult responseCheckResult)
			throws SAMLValidationException
	{
	}

	@Override
	public ResponseTrustCheckResult checkTrust(SAMLVerifiableElement message,
			StatusResponseType response) throws SAMLValidationException
	{
		return new ResponseTrustCheckResult(true);
	}
	
	@Override
	public void checkTrust(AssertionDocument assertionDoc) throws SAMLValidationException
	{
		checkTrust(assertionDoc, new ResponseTrustCheckResult(true));
	}

	@Override
	public CheckingMode getCheckingMode()
	{
		return CheckingMode.REQUIRE_SIGNED_RESPONSE_OR_ASSERTION;
	}
}
