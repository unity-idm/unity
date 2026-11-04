package io.imunity.oauth.as.token.authn;

import java.util.List;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;

import io.imunity.engine.api.authn.CredentialPresentationId;
import io.imunity.engine.api.authn.CredentialPresentationProvider;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.authn.ClientAssertionExchange;
import pl.edu.icm.unity.oauth.as.token.authn.HttpClientAssertionRetrieval;
import pl.edu.icm.unity.oauth.as.token.authn.federation.FederatedPrivateKeyJwtVerificator;
import pl.edu.icm.unity.rest.authn.ext.HttpBasicRetrieval;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;

public final class ClientAuthenticationMethodResolver
{
	private static final List<Mapping> MAPPINGS = List.of(
			new Mapping(HttpBasicRetrieval.PRESENTATION, PasswordExchange.ID,
					ClientAuthenticationMethod.CLIENT_SECRET_BASIC),
			new Mapping(HttpClientAssertionRetrieval.PRESENTATION, ClientAssertionExchange.ID,
					ClientAuthenticationMethod.PRIVATE_KEY_JWT));

	private ClientAuthenticationMethodResolver()
	{
	}

	public static List<ClientAuthenticationMethod> resolve(List<AuthenticationFlow> authenticationFlows,
			OAuthASProperties config)
	{
		return MAPPINGS.stream()
				.filter(mapping -> isSupported(mapping, authenticationFlows, config))
				.map(Mapping::method)
				.toList();
	}

	private static boolean isSupported(Mapping mapping, List<AuthenticationFlow> authenticationFlows,
			OAuthASProperties config)
	{
		return authenticationFlows.stream()
				.flatMap(flow -> flow.getFirstFactorAuthenticators().stream())
				.filter(authenticator -> isAvailable(authenticator, config))
				.anyMatch(authenticator -> supports(authenticator, mapping));
	}

	private static boolean supports(AuthenticatorInstance authenticator, Mapping mapping)
	{
		return authenticator.getRetrieval() instanceof CredentialPresentationProvider provider
				&& provider.getSupportedPresentations().contains(mapping.presentation())
				&& mapping.exchangeId().equals(authenticator.getCredentialVerificator().getExchangeId());
	}

	private static boolean isAvailable(AuthenticatorInstance authenticator, OAuthASProperties config)
	{
		return !(authenticator.getCredentialVerificator() instanceof FederatedPrivateKeyJwtVerificator)
				|| config.getBooleanValue(OAuthASProperties.FEDERATION_MEMBERSHIP_ENABLED);
	}

	private record Mapping(CredentialPresentationId presentation, String exchangeId,
			ClientAuthenticationMethod method)
	{
	}
}
