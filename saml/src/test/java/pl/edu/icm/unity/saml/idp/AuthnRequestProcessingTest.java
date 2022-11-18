/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.messages.XMLExpandedMessage;
import eu.unicore.samly2.proto.AuthnRequest;
import eu.unicore.samly2.trust.EnumeratedTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import org.junit.Test;
import pl.edu.icm.unity.saml.validator.WebAuthRequestValidator;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
				Duration.of(1000L, ChronoUnit.MILLIS),
				new ReplayAttackChecker());
		validator.addKnownRequester("https://unity-sp.example");
		
		XMLExpandedMessage verifiableMessage = new XMLExpandedMessage(request.getXMLBeanDoc(), 
				request.getXMLBeanDoc().getAuthnRequest());
		Throwable error = catchThrowable(() -> validator.validate(request.getXMLBeanDoc(), verifiableMessage));

		assertThat(error).isNull();
	}
}
