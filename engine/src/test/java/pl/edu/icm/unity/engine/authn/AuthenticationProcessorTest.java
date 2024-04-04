/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.identity.SecondFactorOptInService;
import pl.edu.icm.unity.engine.mock.MockPasswordRetrieval;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.DynamicExpressionPolicyConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationProcessorTest
{
	@Mock
	private SecondFactorOptInService secondFactorOptInService;
	@Mock
	private LocalCredentialsRegistry localCred;
	@Mock
	private CredentialRepository credRepo;
	@Mock
	private AuthenticationFlowPolicyConfigMVELContextBuilder policyConfigMVELContextBuilder;

	@InjectMocks
	AuthenticationProcessorImpl processor;

	@Test
	public void shouldGoToSecondFactorWhenDynamicPolicyEvalToTrue() throws Exception
	{
		AuthenticatorInstanceMetadata authenticatorInstanceMetadata = new AuthenticatorInstanceMetadata();
		authenticatorInstanceMetadata.setLocalCredentialName(null);
		authenticatorInstanceMetadata.setTypeDescription(new AuthenticatorTypeDescription(null, null, false));
		CredentialRetrieval credentialRetrieval = new MockPasswordRetrieval();
		AuthenticatorInstance instance = new AuthenticatorImpl(credentialRetrieval, null,
				authenticatorInstanceMetadata);
		AuthenticationOptionKey optionKey = new AuthenticationOptionKey("1", "1");
		AuthenticationResult result = LocalAuthenticationResult
				.successful(new AuthenticatedEntity(1L, AuthenticationSubject.entityBased(1L), null));
		AuthenticationFlow authenticationFlow = new AuthenticationFlow("flow", Policy.DYNAMIC_EXPRESSION, Set.of(),
				List.of(instance), new DynamicExpressionPolicyConfiguration("hasValid2FCredential == true"), 1L);
		when(policyConfigMVELContextBuilder.createMvelContext(optionKey, result, false,
				authenticationFlow)).thenReturn(Map.of("hasValid2FCredential", true));
		PartialAuthnState processPrimaryAuthnResult = processor.processPrimaryAuthnResult(result, authenticationFlow,
				optionKey);
		assertThat(processPrimaryAuthnResult.getSecondaryAuthenticator()).isNotNull();

	}
	
	@Test
	public void shouldSkipSecondFactorWhenDynamicPolicyEvalToFalse() throws Exception
	{
		AuthenticatorInstanceMetadata authenticatorInstanceMetadata = new AuthenticatorInstanceMetadata();
		authenticatorInstanceMetadata.setLocalCredentialName(null);
		authenticatorInstanceMetadata.setTypeDescription(new AuthenticatorTypeDescription(null, null, false));
		CredentialRetrieval credentialRetrieval = new MockPasswordRetrieval();
		AuthenticatorInstance instance = new AuthenticatorImpl(credentialRetrieval, null,
				authenticatorInstanceMetadata);
		AuthenticationOptionKey optionKey = new AuthenticationOptionKey("1", "1");
		AuthenticationResult result = LocalAuthenticationResult
				.successful(new AuthenticatedEntity(1L, AuthenticationSubject.entityBased(1L), null));
		AuthenticationFlow authenticationFlow = new AuthenticationFlow("flow", Policy.DYNAMIC_EXPRESSION, Set.of(),
				List.of(instance), new DynamicExpressionPolicyConfiguration("hasValid2FCredential == true"), 1L);
		when(policyConfigMVELContextBuilder.createMvelContext(optionKey, result, false,
				authenticationFlow)).thenReturn(Map.of("hasValid2FCredential", false));
		PartialAuthnState processPrimaryAuthnResult = processor.processPrimaryAuthnResult(result, authenticationFlow,
				optionKey);
		assertThat(processPrimaryAuthnResult.getSecondaryAuthenticator()).isNull();
	}
}
