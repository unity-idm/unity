/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import eu.unicore.samly2.messages.XMLExpandedMessage;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class SAMLResponseVerificatorTest
{
	@Test
	public void shouldAcceptAuthnAssertionWithoutNameId() throws Exception
	{
		RemoteAuthnResultTranslator translator = mock(RemoteAuthnResultTranslator.class);
		TranslationProfile profile = mock(TranslationProfile.class);
		RemoteAuthenticationResult result = mock(RemoteAuthenticationResult.class);
		when(translator.getTranslatedResult(any(), eq(profile), eq(false), any(), eq(null), eq(false), any()))
			.thenReturn(result);
			
		SAMLResponseVerificator verificator = new SAMLResponseVerificator(
				mock(ReplayAttackChecker.class), 
				"https://192.168.0.10:2443/unitygw/spSAMLResponseConsumer", 
				translator);
		
		RemoteAuthnContext remoteAuthnState = createRemoteContext();
		
		AuthenticationResult authnResult = verificator.processResponse(remoteAuthnState, profile, AuthenticationMethod.u_saml);
		
		assertThat(authnResult).isEqualTo(result);
	}
	
	private RemoteAuthnContext createRemoteContext() throws Exception
	{
		SAMLSPConfiguration config = createConfig(); 
		RedirectedAuthnState baseState = new RedirectedAuthnState(
				new AuthenticationStepContext(new AuthenticationRealm(), 
						mock(AuthenticationFlow.class), 
						AuthenticationOptionKey.authenticatorOnlyKey("authnKey"), 
						FactorOrder.FIRST, 
						"endpointPath"), 
				null, 
				new LoginMachineDetails("ip", "os", "browser"), 
				"ultimateReturnURL", 
				AuthenticationTriggeringContext.authenticationTriggeredFirstFactor());
		RemoteAuthnContext ret = new RemoteAuthnContext(getTrustedIdPConfig(), config, baseState,
				null, null, null);
		String response = loadResponse();
		ResponseDocument responseDocument = ResponseDocument.Factory.parse(response);
		ret.setResponse(response, Binding.HTTP_POST, new XMLExpandedMessage(responseDocument, 
				responseDocument.getResponse()));
		return ret;
	}
	
	private String loadResponse()
	{
		try
		{
			return FileUtils.readFileToString(
					new File("src/test/resources/responseDocWithoutSubjectNameId.xml"), 
					StandardCharsets.UTF_8);
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private SAMLSPConfiguration createConfig() throws EngineException
	{
		return FakeSAMLSPConfiguration.getFakeBuilder()
				.withTrustCheckerFactory(idp -> new TrustAllTrustChecker())
				.build();
	}
	
	private TrustedIdPConfiguration getTrustedIdPConfig() throws EngineException
	{
		return FakeTrustedIdPConfiguration.getFakeBuilder()
			.withSamlId("idp")
			.withBinding(Binding.HTTP_POST)
			.withPublicKeys(List.of(mock(PublicKey.class)))
			.build();
	}
}