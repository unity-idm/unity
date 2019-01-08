/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.proto.AuthnRequest;
import eu.unicore.samly2.trust.EnumeratedTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.saml.validator.WebAuthRequestValidator;

public class AuthnRequestProcessingTest
{
	@Test
	public void shouldAcceptAuthnRequestWithoutConsumerURL() throws SAMLServerException
	{
		AuthnRequest request = new AuthnRequest(
				new NameID("https://unity-sp.example", SAMLConstants.NFORMAT_ENTITY).getXBean());
		
		EnumeratedTrustChecker authnTrustChecker = new EnumeratedTrustChecker();
		authnTrustChecker.addTrustedIssuer("https://unity-sp.example", 
				"https://unity-sp.example/return");
		WebAuthRequestValidator validator = new WebAuthRequestValidator(
				"https://unity-idp.example", 
				authnTrustChecker, 
				1000l, 
				new ReplayAttackChecker());
		validator.addKnownRequester("https://unity-sp.example");
		
		catchException(validator).validate(request.getXMLBeanDoc());

		assertThat(caughtException(), is(nullValue()));
	}
}
