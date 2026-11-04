package io.imunity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;

import io.imunity.oauth.as.token.authn.ClientAuthenticationMethodResolverTest.Scenario;
import jakarta.ws.rs.core.Response;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockPKIMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthRequestValidator.OAuthRequestValidatorFactory;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.oauth.as.federation.OAuthASFederationEntityStatementResource;
import pl.edu.icm.unity.oauth.as.token.CredentialJwkConverter;
import pl.edu.icm.unity.oauth.as.token.DiscoveryResource;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.token.access.AccessTokenResource;
import pl.edu.icm.unity.oauth.as.token.access.AccessTokenResourceFactory;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.oauth.as.token.introspection.TokenIntrospectionResource;
import pl.edu.icm.unity.oauth.as.token.introspection.TokenIntrospectionResource.TokenIntrospectionResourceFactory;

class OAuthTokenEndpointMetadataTest
{
	private static final String ISSUER = "https://localhost:233/oauth-token";
	private static final String METHODS = "token_endpoint_auth_methods_supported";

	@ParameterizedTest(name = "{0}")
	@MethodSource("io.imunity.oauth.as.token.authn.ClientAuthenticationMethodResolverTest#metadataScenarios")
	void shouldAdvertiseSameConfiguredMethodsInDiscoveryAndSignedFederationMetadata(Scenario scenario) throws Exception
	{
		OAuthTokenEndpoint endpoint = endpoint();
		initialize(endpoint, scenario.flows(), scenario.federationEnabled());
		MetadataResources resources = resources(endpoint);

		JSONObject discovery = discovery(resources);
		Response federationResponse = resources.federation().getEntityStatement();

		assertThat(federationResponse.getStatus()).isEqualTo(200);
		EntityStatement statement = EntityStatement.parse(SignedJWT.parse(federationResponse.getEntity().toString()));
		statement.verifySignature(new JWKSet(
				CredentialJwkConverter.buildPublicJWK(new MockPKIMan().getCredential("MAIN").getCertificate())));
		JSONObject federation = statement.getClaimsSet().getMetadata(EntityType.OPENID_PROVIDER);
		List<String> expected = scenario.expected().stream().map(ClientAuthenticationMethod::getValue).toList();
		assertThat(discovery).containsEntry(METHODS, expected);
		assertThat(federation).containsEntry(METHODS, expected);
	}

	@Test
	void shouldRecomputeMethodsWhenEndpointIsReinitializedWithChangedFlows() throws Exception
	{
		OAuthTokenEndpoint endpoint = endpoint();
		initialize(endpoint, List.of(ClientAuthenticationMethodResolverTest.flow(
				ClientAuthenticationMethodResolverTest.basicAuthenticator())), false);
		assertThat(discovery(resources(endpoint))).containsEntry(METHODS, List.of("client_secret_basic"));

		initialize(endpoint, List.of(ClientAuthenticationMethodResolverTest.flow(
				ClientAuthenticationMethodResolverTest.jwtAuthenticator(false))), false);
		assertThat(discovery(resources(endpoint))).containsEntry(METHODS, List.of("private_key_jwt"));

		initialize(endpoint, List.of(), false);
		assertThat(discovery(resources(endpoint))).containsEntry(METHODS, List.of());
	}

	@Test
	void shouldKeepMethodsIsolatedBetweenTokenEndpoints() throws Exception
	{
		OAuthTokenEndpoint basicEndpoint = endpoint();
		initialize(basicEndpoint, List.of(ClientAuthenticationMethodResolverTest.flow(
				ClientAuthenticationMethodResolverTest.basicAuthenticator())), false);
		OAuthTokenEndpoint jwtEndpoint = endpoint();
		initialize(jwtEndpoint, List.of(ClientAuthenticationMethodResolverTest.flow(
				ClientAuthenticationMethodResolverTest.jwtAuthenticator(false))), false);

		JSONObject basicMetadata = discovery(resources(basicEndpoint));
		JSONObject jwtMetadata = discovery(resources(jwtEndpoint));

		assertThat(basicMetadata).containsEntry(METHODS, List.of("client_secret_basic"));
		assertThat(jwtMetadata).containsEntry(METHODS, List.of("private_key_jwt"));
	}

	private OAuthTokenEndpoint endpoint() throws Exception
	{
		OAuthEndpointsCoordinator coordinator = new OAuthEndpointsCoordinator();
		coordinator.registerAuthzEndpoint(ISSUER, "https://localhost:233/oauth-authz");
		OAuthScopesService scopeService = mock(OAuthScopesService.class);
		when(scopeService.getActiveScopeNames(any())).thenReturn(List.of());
		AccessTokenResourceFactory accessFactory = mock(AccessTokenResourceFactory.class);
		when(accessFactory.getHandler(any(), any())).thenReturn(mock(AccessTokenResource.class));
		TokenIntrospectionResourceFactory introspectionFactory = mock(TokenIntrospectionResourceFactory.class);
		when(introspectionFactory.getTokenIntrospection(any())).thenReturn(mock(TokenIntrospectionResource.class));
		URL advertisedAddress = URI.create("https://localhost:233").toURL();
		return new OAuthTokenEndpoint(mock(MessageSource.class), mock(SessionManagement.class),
				mock(NetworkServer.class), new MockPKIMan(), coordinator, mock(AuthenticationProcessor.class),
				mock(EntityManagement.class), mock(EntityManagement.class), mock(AttributesManagement.class),
				mock(IdPEngine.class), mock(TokensManagement.class), mock(OAuthAccessTokenRepository.class),
				mock(OAuthRefreshTokenRepository.class), () -> advertisedAddress,
				scopeService, accessFactory, introspectionFactory, mock(OAuthRequestValidatorFactory.class),
				mock(DeviceCodeRepository.class));
	}

	private void initialize(OAuthTokenEndpoint endpoint, List<AuthenticationFlow> flows, boolean federationEnabled)
			throws Exception
	{
		Properties properties = new Properties();
		properties.setProperty(OAuthASProperties.P + OAuthASProperties.ISSUER_URI, ISSUER);
		properties.setProperty(OAuthASProperties.P + OAuthASProperties.CREDENTIAL, "MAIN");
		properties.setProperty(OAuthASProperties.P + OAuthASProperties.FEDERATION_CREDENTIAL, "MAIN");
		properties.setProperty(OAuthASProperties.P + OAuthASProperties.FEDERATION_MEMBERSHIP_ENABLED,
				Boolean.toString(federationEnabled));
		StringWriter serialized = new StringWriter();
		properties.store(serialized, null);
		Endpoint definition = new Endpoint("token", OAuthTokenEndpoint.NAME, "/oauth-token", null, 0);
		ResolvedEndpoint description = new ResolvedEndpoint(definition, mock(AuthenticationRealm.class),
				OAuthTokenEndpoint.TYPE);
		endpoint.initialize(description, flows, serialized.toString());
	}

	private MetadataResources resources(OAuthTokenEndpoint endpoint)
	{
		DiscoveryResource discovery = null;
		OAuthASFederationEntityStatementResource federation = null;
		for (Object resource : endpoint.new OAuthTokenJAXRSApp().getSingletons())
		{
			if (resource instanceof DiscoveryResource discoveryResource)
				discovery = discoveryResource;
			if (resource instanceof OAuthASFederationEntityStatementResource federationResource)
				federation = federationResource;
		}
		assertThat(discovery).isNotNull();
		assertThat(federation).isNotNull();
		return new MetadataResources(discovery, federation);
	}

	private JSONObject discovery(MetadataResources resources) throws Exception
	{
		Response response = resources.discovery().getMetadata();
		assertThat(response.getStatus()).isEqualTo(200);
		return JSONObjectUtils.parse(response.getEntity().toString());
	}

	private record MetadataResources(DiscoveryResource discovery, OAuthASFederationEntityStatementResource federation)
	{
	}
}
