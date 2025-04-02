/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.claims.ACR;
import com.nimbusds.openid.connect.sdk.op.ACRRequest;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;

public class EssentialACRConsistencyValidatorTest
{
	@Test
	public void shouldThrowExceptionWhenReturnedACRNotContainsRequestedACR() throws OAuthErrorResponseException, URISyntaxException
	{
		OAuthAuthzContext context = new OAuthAuthzContext(new AuthorizationRequest(new URI(""), ResponseType.TOKEN, new ClientID()), null);
		context.setRequestedAcr(new ACRRequest(List.of(new ACR("essentialACR1")), null));
		context.setReturnURI(new URI("return"));
		List<DynamicAttribute> attrs = List.of(new DynamicAttribute(new Attribute("acr", null, null, List.of("acr1"))));
		assertThrows(OAuthErrorResponseException.class, () -> EssentialACRConsistencyValidator.verifyEssentialRequestedACRisReturned(context, attrs));
	}
	
	@Test
	public void shouldValidateSuccessfulltWhenACRContainsRequestedACR() throws OAuthErrorResponseException, URISyntaxException
	{
		OAuthAuthzContext context = new OAuthAuthzContext(new AuthorizationRequest(new URI(""), ResponseType.TOKEN, new ClientID()), null);
		context.setRequestedAcr(new ACRRequest(List.of(new ACR("essentialACR1")), null));
		context.setReturnURI(new URI("return"));
		List<DynamicAttribute> attrs = List.of(new DynamicAttribute(new Attribute("acr", null, null, List.of("essentialACR1"))));
		assertDoesNotThrow(() -> EssentialACRConsistencyValidator.verifyEssentialRequestedACRisReturned(context, attrs));
	}
	
	@Test
	public void shouldValidateSuccessfulltWhenRequestedACRisVoluntary() throws OAuthErrorResponseException, URISyntaxException
	{
		OAuthAuthzContext context = new OAuthAuthzContext(new AuthorizationRequest(new URI(""), ResponseType.TOKEN, new ClientID()), null);
		context.setRequestedAcr(new ACRRequest(List.of(), List.of(new ACR("voluntaryACR1"))));
		context.setReturnURI(new URI("return"));
		List<DynamicAttribute> attrs = List.of(new DynamicAttribute(new Attribute("acr", null, null, List.of("anotherACR"))));
		assertDoesNotThrow(() -> EssentialACRConsistencyValidator.verifyEssentialRequestedACRisReturned(context, attrs));
	}
}
