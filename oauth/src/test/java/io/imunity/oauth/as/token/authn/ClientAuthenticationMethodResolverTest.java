package io.imunity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;

import io.imunity.engine.api.authn.CredentialPresentationId;
import io.imunity.engine.api.authn.CredentialPresentationProvider;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.token.authn.ClientAssertionExchange;
import pl.edu.icm.unity.oauth.as.token.authn.HttpClientAssertionRetrieval;
import pl.edu.icm.unity.oauth.as.token.authn.federation.FederatedPrivateKeyJwtVerificator;
import pl.edu.icm.unity.oauth.as.token.authn.local.PrivateKeyJwtVerificator;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrieval;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;

class ClientAuthenticationMethodResolverTest
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("scenarios")
	void shouldResolveMethodsFromConfiguredPresentationAndExchange(Scenario scenario)
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.FEDERATION_MEMBERSHIP_ENABLED,
				Boolean.toString(scenario.federationEnabled()));

		List<ClientAuthenticationMethod> methods =
				ClientAuthenticationMethodResolver.resolve(scenario.flows(), config);

		assertThat(methods).containsExactlyElementsOf(scenario.expected());
	}

	private static Stream<Scenario> scenarios()
	{
		AuthenticatorInstance basic = basicAuthenticator();
		AuthenticatorInstance localJwt = jwtAuthenticator(false);
		AuthenticatorInstance federatedJwt = jwtAuthenticator(true);
		CredentialRetrieval pluginRetrieval = presentationRetrieval(
				new CredentialPresentationId(HttpBasicRetrieval.PRESENTATION.value()));

		return Stream.of(
				Scenario.builder("no configured flows").build(),
				Scenario.builder("Basic only").flows(flow(basic))
						.expected(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).build(),
				Scenario.builder("local JWT without federation").flows(flow(localJwt))
						.expected(ClientAuthenticationMethod.PRIVATE_KEY_JWT).build(),
				Scenario.builder("federated JWT enabled").flows(flow(federatedJwt)).federationEnabled()
						.expected(ClientAuthenticationMethod.PRIVATE_KEY_JWT).build(),
				Scenario.builder("federated JWT disabled").flows(flow(federatedJwt)).build(),
				Scenario.builder("local JWT survives disabled federation")
						.flows(flow(federatedJwt, localJwt))
						.expected(ClientAuthenticationMethod.PRIVATE_KEY_JWT).build(),
				Scenario.builder("multiple flows, stable order and duplicates")
						.flows(flow(localJwt), flow(basic), flow(basicAuthenticator(), jwtAuthenticator(false)))
						.expected(ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
								ClientAuthenticationMethod.PRIVATE_KEY_JWT).build(),
				Scenario.builder("legacy retrieval without presentation capability")
						.flows(flow(authenticator(mock(CredentialRetrieval.class), PasswordExchange.ID))).build(),
				Scenario.builder("unknown plugin presentation")
						.flows(flow(authenticator(presentationRetrieval(
								new CredentialPresentationId("urn:example:custom")), PasswordExchange.ID))).build(),
				Scenario.builder("plugin implementing the Basic presentation contract")
						.flows(flow(authenticator(pluginRetrieval, PasswordExchange.ID)))
						.expected(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).build(),
				Scenario.builder("unknown verification exchange")
						.flows(flow(authenticator(new HttpBasicRetrieval(), "custom exchange"))).build(),
				Scenario.builder("mismatched exchanges cannot be combined across authenticators")
						.flows(flow(authenticator(new HttpBasicRetrieval(), ClientAssertionExchange.ID),
								authenticator(new HttpClientAssertionRetrieval(), PasswordExchange.ID))).build(),
				Scenario.builder("empty presentation capability")
						.flows(flow(authenticator(presentationRetrieval(), PasswordExchange.ID))).build(),
				Scenario.builder("multiple presentations are constrained by the exchange")
						.flows(flow(authenticator(presentationRetrieval(HttpBasicRetrieval.PRESENTATION,
								HttpClientAssertionRetrieval.PRESENTATION), PasswordExchange.ID)))
						.expected(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).build(),
				Scenario.builder("JWT configured solely as second factor")
						.flows(new AuthenticationFlow("flow", Policy.REQUIRE, Set.of(basic), List.of(localJwt),
								AuthenticationFlowDefinition.EMPTY_CONFIGURATION, 0))
						.expected(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).build(),
				Scenario.builder("no primary authenticator")
						.flows(new AuthenticationFlow("flow", Policy.NEVER, Set.of(), List.of(localJwt),
								AuthenticationFlowDefinition.EMPTY_CONFIGURATION, 0)).build());
	}

	static AuthenticatorInstance basicAuthenticator()
	{
		return authenticator(new HttpBasicRetrieval(), PasswordExchange.ID);
	}

	static Stream<Scenario> metadataScenarios()
	{
		return Stream.of(
				Scenario.builder("no client authentication methods").federationEnabled().build(),
				Scenario.builder("Basic only").flows(flow(basicAuthenticator())).federationEnabled()
						.expected(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).build(),
				Scenario.builder("local JWT only").flows(flow(jwtAuthenticator(false))).federationEnabled()
						.expected(ClientAuthenticationMethod.PRIVATE_KEY_JWT).build(),
				Scenario.builder("federated JWT only").flows(flow(jwtAuthenticator(true))).federationEnabled()
						.expected(ClientAuthenticationMethod.PRIVATE_KEY_JWT).build(),
				Scenario.builder("Basic and JWT").flows(flow(jwtAuthenticator(false), basicAuthenticator()))
						.federationEnabled().expected(ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
								ClientAuthenticationMethod.PRIVATE_KEY_JWT).build());
	}

	static AuthenticatorInstance jwtAuthenticator(boolean federated)
	{
		CredentialVerificator verificator = federated ? mock(FederatedPrivateKeyJwtVerificator.class)
				: mock(PrivateKeyJwtVerificator.class);
		when(verificator.getExchangeId()).thenReturn(ClientAssertionExchange.ID);
		return authenticator(new HttpClientAssertionRetrieval(), verificator);
	}

	static AuthenticationFlow flow(AuthenticatorInstance... authenticators)
	{
		return new AuthenticationFlow("flow", Policy.NEVER, new LinkedHashSet<>(List.of(authenticators)),
				List.of(), AuthenticationFlowDefinition.EMPTY_CONFIGURATION, 0);
	}

	private static AuthenticatorInstance authenticator(CredentialRetrieval retrieval, String exchangeId)
	{
		CredentialVerificator verificator = mock(CredentialVerificator.class);
		when(verificator.getExchangeId()).thenReturn(exchangeId);
		return authenticator(retrieval, verificator);
	}

	private static AuthenticatorInstance authenticator(CredentialRetrieval retrieval, CredentialVerificator verificator)
	{
		AuthenticatorInstance authenticator = mock(AuthenticatorInstance.class);
		when(authenticator.getRetrieval()).thenReturn(retrieval);
		when(authenticator.getCredentialVerificator()).thenReturn(verificator);
		return authenticator;
	}

	private static CredentialRetrieval presentationRetrieval(CredentialPresentationId... presentations)
	{
		CredentialRetrieval retrieval = mock(CredentialRetrieval.class,
				withSettings().extraInterfaces(CredentialPresentationProvider.class));
		when(((CredentialPresentationProvider) retrieval).getSupportedPresentations())
				.thenReturn(Set.of(presentations));
		return retrieval;
	}

	record Scenario(String description, List<AuthenticationFlow> flows, boolean federationEnabled,
			List<ClientAuthenticationMethod> expected)
	{
		static Builder builder(String description)
		{
			return new Builder(description);
		}

		@Override
		public String toString()
		{
			return description;
		}

		private static class Builder
		{
			private final String description;
			private List<AuthenticationFlow> flows = List.of();
			private boolean federationEnabled;
			private List<ClientAuthenticationMethod> expected = List.of();

			private Builder(String description)
			{
				this.description = description;
			}

			Builder flows(AuthenticationFlow... flows)
			{
				this.flows = List.of(flows);
				return this;
			}

			Builder federationEnabled()
			{
				federationEnabled = true;
				return this;
			}

			Builder expected(ClientAuthenticationMethod... methods)
			{
				expected = List.of(methods);
				return this;
			}

			Scenario build()
			{
				return new Scenario(description, flows, federationEnabled, expected);
			}
		}
	}
}
