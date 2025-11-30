package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.TokenTypeURI;
import com.nimbusds.oauth2.sdk.tokenexchange.TokenExchangeGrant;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.webauthz.ClaimsInTokenAttribute;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

/**
 * An integration test of token exchange flow
 * 
 * @author P.Piernik
 *
 */
public class ExchangeTokenTest extends TokenTestBase
{
	private ClientAuthentication ca1;
	private ClientAuthentication ca2;

	@BeforeEach
	public void initClients()
	{
		ca1 = client("client1");
		ca2 = client("client2");
	}

	@Test
	public void shouldExchangeTokenWithSkippedRequestedTokenExchangeScope() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("bar", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("bar", AccessTokenResource.EXCHANGE_SCOPE)
				.withAudience("client2")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));
		JSONObject info = getTokenInfo(parsed.getTokens()
				.getAccessToken());

		assertThat(info.get("sub")).isEqualTo("userA");
		assertThat(info.get("client_id")).isEqualTo("client2");
		assertThat(info.get("aud")).isEqualTo("client2");
		assertThat(((JSONArray) info.get("scope"))).containsExactly("bar");
		assertThat(info.get("exp")).isNotNull();
	}

	@Test
	public void shouldDenyToExchangeTokenWithRequestedEnhancedScopes() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("bar", "foo", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("bar", "new scope")
				.withAudience("client2")
				.build();

		assertBadRequestWithInvalidScope(req);
	}

	@Test
	public void shouldExchangeTokenWithScopeConcretization() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(List.of(new RequestedOAuthScope(AccessTokenResource.EXCHANGE_SCOPE,
				ActiveOAuthScopeDefinition.builder()
						.withName(AccessTokenResource.EXCHANGE_SCOPE)
						.withDescription(AccessTokenResource.EXCHANGE_SCOPE)
						.withAttributes(List.of())
						.build(),
				false),
				new RequestedOAuthScope("read:files/.*", ActiveOAuthScopeDefinition.builder()
						.withName("read:files/.*")
						.withDescription("\"read:files/.*\"")
						.withAttributes(List.of())
						.withPattern(true)
						.build(), true), 
				new RequestedOAuthScope("foo", ActiveOAuthScopeDefinition.builder()
						.withName("foo")
						.withDescription("foo")
						.withAttributes(List.of("email"))
						.withPattern(false)
						.build(), true)));

		TokenRequest req = exchange(at).withScopes("read:files/dir1/.*", "foo")
				.withAudience("client2")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));
		JSONObject info = getTokenInfo(parsed.getTokens()
				.getAccessToken());

		assertThat(info.get("sub")).isEqualTo("userA");
		assertThat(info.get("client_id")).isEqualTo("client2");
		assertThat(info.get("aud")).isEqualTo("client2");
		assertThat(((JSONArray) info.get("scope"))).containsExactlyInAnyOrder("read:files/dir1/.*", "foo");
		assertThat(info.get("exp")).isNotNull();
	}
	
	@Test
	public void shouldDenyToExchangeTokenWithConcretizationWithScopeWhichIsNotPatternScope() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(List.of(new RequestedOAuthScope(AccessTokenResource.EXCHANGE_SCOPE,
				ActiveOAuthScopeDefinition.builder()
						.withName(AccessTokenResource.EXCHANGE_SCOPE)
						.withDescription(AccessTokenResource.EXCHANGE_SCOPE)
						.withAttributes(List.of())
						.build(),
				false),
				new RequestedOAuthScope("read:files/.*", ActiveOAuthScopeDefinition.builder()
						.withName("read:files/.*")
						.withDescription("\"read:files/.*\"")
						.withAttributes(List.of())
						.withPattern(true)
						.build(), false)));

		TokenRequest req = exchange(at).withScopes("read:files/dir1/.*")
				.withAudience("client2")
				.build();
		assertBadRequestWithInvalidScope(req);
	}
	
	@Test
	public void shouldDenyExchangeTokenWithInvalidScopeConcretization() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(List.of(new RequestedOAuthScope(AccessTokenResource.EXCHANGE_SCOPE,
				ActiveOAuthScopeDefinition.builder()
						.withName(AccessTokenResource.EXCHANGE_SCOPE)
						.withDescription(AccessTokenResource.EXCHANGE_SCOPE)
						.withAttributes(List.of())
						.build(),
				false),
				new RequestedOAuthScope("read:files/.*", ActiveOAuthScopeDefinition.builder()
						.withName("read:files/.*")
						.withDescription("\"read:files/.*\"")
						.withAttributes(List.of())
						.withPattern(true)
						.build(), true)));

		TokenRequest req = exchange(at).withScopes("read:filess/dir1/.*")
				.withAudience("client2")
				.build();

		assertBadRequestWithInvalidScope(req);
	}
	
	
	@Test
	public void shouldExchangeTokenWithRequestedNarrowedScopes() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("bar", "foo", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("bar")
				.withAudience("client2")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));
		JSONObject info = getTokenInfo(parsed.getTokens()
				.getAccessToken());

		assertThat(info.get("sub")).isEqualTo("userA");
		assertThat(info.get("client_id")).isEqualTo("client2");
		assertThat(info.get("aud")).isEqualTo("client2");
		assertThat(((JSONArray) info.get("scope"))).containsExactly("bar");
		assertThat(info.get("exp")).isNotNull();
	}

	@Test
	public void shouldDenyToExchangeTokenWithActorParameter() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("bar", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("bar")
				.withAudience("client2")
				.withActor(new BearerAccessToken())
				.withActorTokenType(TokenTypeURI.ACCESS_TOKEN)
				.build();

		assertBadRequestWithInvalidParams(req);
	}

	@Test
	public void shouldDenyToExchangeTokenWithIncorrectAudience() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("bar", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("bar")
				.withAudience("client3")
				.build();

		assertBadRequestWithInvalidParams(req);
	}

	@Test
	public void shouldDenyToExchangeTokenWithIncorrectRequestedTokenType() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("bar", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).forType(TokenTypeURI.parse("wrong"))
				.withScopes("bar")
				.withAudience("client2")
				.build();

		assertBadRequestWithInvalidParams(req);
	}

	@Test
	public void shouldExchangeTokenWithIdToken() throws Exception
	{
		setupOIDC(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("openid", "foo", "bar", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("openid", "foo", "bar")
				.forType(TokenTypeURI.ID_TOKEN)
				.withAudience("client2")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));

		assertThat(parsed.getTokens()
				.getAccessToken()).isNotNull();
		assertThat(parsed.getCustomParameters()
				.get("id_token")).isNotNull();
	}

	@Test
	public void shouldExchangeAccessTokenWithoutIdToken() throws Exception
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("foo", "bar", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("foo", "bar")
				.withAudience("client2")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));

		assertThat(parsed.getTokens()
				.getAccessToken()).isNotNull();
		assertThat(parsed.getTokens()
				.getAccessToken()
				.getIssuedTokenType()
				.getURI()
				.toASCIIString()).isEqualTo(AccessTokenResource.ACCESS_TOKEN_TYPE_ID);
		assertThat(parsed.getCustomParameters()
				.get("id_token")).isNull();
	}

	@Test
	public void shouldExchangeTokenWithIdTokenRespectingAudienceAndResource() throws Exception
	{
		setupOIDC(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("openid", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("openid")
				.withAudience("client2")
				.forType(TokenTypeURI.ID_TOKEN)
				.withResources("https://resource1.uri", "https://resource2.uri")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));

		assertThat(parsed.getTokens()
				.getAccessToken()).isNotNull();
		assertThat(parsed.getCustomParameters()
				.get("id_token")).isNotNull();
		assertThat(parsed.getTokens()
				.getAccessToken()
				.getIssuedTokenType()
				.getURI()
				.toASCIIString()).isEqualTo(AccessTokenResource.ACCESS_TOKEN_TYPE_ID);

		SignedJWT idToken = SignedJWT.parse(parsed.getCustomParameters()
				.get("id_token")
				.toString());
		assertThat(idToken.getJWTClaimsSet()
				.getAudience()).containsExactlyInAnyOrder("client2", "https://resource1.uri", "https://resource2.uri");

		JSONObject info = getTokenInfo(parsed.getTokens()
				.getAccessToken());
		assertThat(info.get("sub")).isEqualTo("userA");
		assertThat(info.get("client_id")).isEqualTo("client2");
		assertThat((JSONArray) info.get("aud")).containsExactlyInAnyOrder("client2", "https://resource1.uri", "https://resource2.uri");
		assertThat(((JSONArray) info.get("scope"))).containsExactlyInAnyOrder("openid");
		assertThat(info.get("exp")).isNotNull();
	}

	@Test
	public void shouldExchangeTokenWithClientIdInAudOnlyIfOpenId() throws Exception
	{
		setupOIDC(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("openid", AccessTokenResource.EXCHANGE_SCOPE));

		TokenRequest req = exchange(at).withScopes("openid")
				.withAudience("client2")
				.forType(TokenTypeURI.ID_TOKEN)
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));

		SignedJWT access = SignedJWT.parse(parsed.getTokens()
				.getAccessToken()
				.getValue()
				.toString());
		assertThat(access.getJWTClaimsSet()
				.getAudience()).containsExactlyInAnyOrder("client2");
	}

	@Test
	public void shouldExchangeTokenRespectingClaimInTokensFromOriginalToken() throws Exception
	{
		setupOIDC(RefreshTokenIssuePolicy.ALWAYS);

		AccessTokenResponse original = init(Set.of("openid", AccessTokenResource.EXCHANGE_SCOPE, "sc1"), ca1, null,
				ClaimsInTokenAttribute.builder()
						.withValues(Set.of(ClaimsInTokenAttribute.Value.token, ClaimsInTokenAttribute.Value.id_token))
						.build());

		AccessToken at = original.getTokens()
				.getAccessToken();

		SignedJWT idOrig = SignedJWT.parse(original.getCustomParameters()
				.get("id_token")
				.toString());
		SignedJWT atOrig = SignedJWT.parse(original.getTokens()
				.getAccessToken()
				.getValue()
				.toString());

		assertThat(idOrig.getJWTClaimsSet()
				.getClaimAsString("email")).contains("example@example.com");
		assertThat(atOrig.getJWTClaimsSet()
				.getClaimAsString("email")).contains("example@example.com");

		TokenRequest req = exchange(at).forType(TokenTypeURI.ID_TOKEN)
				.withScopes("openid", "sc1")
				.withAudience("client2")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));

		SignedJWT idEx = SignedJWT.parse(parsed.getCustomParameters()
				.get("id_token")
				.toString());
		SignedJWT atEx = SignedJWT.parse(parsed.getTokens()
				.getAccessToken()
				.getValue()
				.toString());

		assertThat(idEx.getJWTClaimsSet()
				.getAudience()).containsExactlyInAnyOrder("client2");
		assertThat(idEx.getJWTClaimsSet()
				.getClaimAsString("email")).contains("example@example.com");
		assertThat(atEx.getJWTClaimsSet()
				.getClaimAsString("email")).contains("example@example.com");
	}

	@Test
	public void shouldExchangeTokenWithoutOriginalAudience() throws Exception
	{
		setupOIDC(RefreshTokenIssuePolicy.ALWAYS);

		AccessToken at = issueToken(Set.of("openid", AccessTokenResource.EXCHANGE_SCOPE),
				List.of("additionalAudience1", "additionalAudience2"));

		TokenRequest req = exchange(at).forType(TokenTypeURI.ID_TOKEN)
				.withScopes("openid")
				.withAudience("client2")
				.withResources("https://resource1.uri", "https://resource2.uri")
				.build();

		AccessTokenResponse parsed = AccessTokenResponse.parse(exec(req));

		SignedJWT idToken = SignedJWT.parse(parsed.getCustomParameters()
				.get("id_token")
				.toString());
		assertThat(idToken.getJWTClaimsSet()
				.getAudience()).containsExactlyInAnyOrder("client2", "https://resource1.uri", "https://resource2.uri");

		JSONObject info = getTokenInfo(parsed.getTokens()
				.getAccessToken());
		assertThat(info.get("sub")).isEqualTo("userA");
		assertThat(info.get("client_id")).isEqualTo("client2");
		assertThat((JSONArray) info.get("aud")).containsExactlyInAnyOrder("client2", "https://resource1.uri", "https://resource2.uri");
		assertThat(((JSONArray) info.get("scope"))).containsExactlyInAnyOrder("openid");
		assertThat(info.get("exp")).isNotNull();
	}

	private ClientAuthentication client(String id)
	{
		return new ClientSecretBasic(new ClientID(id), new Secret("clientPass"));
	}

	private AccessToken issueToken(Set<String> scopes) throws Exception
	{
		return init(scopes, ca1, null, null).getTokens()
				.getAccessToken();
	}

	private AccessToken issueToken(List<RequestedOAuthScope> scopes) throws Exception
	{
		return init(scopes, ca1, null, null).getTokens()
				.getAccessToken();
	}
	
	
	private AccessToken issueToken(Set<String> scopes, List<String> additionalAudience) throws Exception
	{
		return init(scopes, ca1, additionalAudience, null).getTokens()
				.getAccessToken();
	}

	private class ExchangeReqBuilder
	{
		private final AccessToken token;
		private TokenTypeURI requestedType;
		private List<String> scopes;
		private List<String> audience;
		private List<URI> resources;
		private AccessToken actorToken;
		private TokenTypeURI actorTokenType;

		private ExchangeReqBuilder(AccessToken t)
		{
			this.token = t;
		}

		ExchangeReqBuilder forType(TokenTypeURI t)
		{
			this.requestedType = t;
			return this;
		}

		ExchangeReqBuilder withScopes(String... s)
		{
			this.scopes = List.of(s);
			return this;
		}

		ExchangeReqBuilder withAudience(String... a)
		{
			this.audience = List.of(a);
			return this;
		}

		ExchangeReqBuilder withResources(String... uris)
		{
			this.resources = uris == null ? null
					: List.of(uris)
							.stream()
							.map(URI::create)
							.toList();
			return this;
		}

		ExchangeReqBuilder withActor(AccessToken actor)
		{
			this.actorToken = actor;
			return this;
		}

		ExchangeReqBuilder withActorTokenType(TokenTypeURI tokenType)
		{
			this.actorTokenType = tokenType;
			return this;
		}

		TokenRequest build() throws Exception
		{
			return new TokenRequest(new URI(getOauthUrl("/oauth/token")), ca2,
					new TokenExchangeGrant(token, TokenTypeURI.ACCESS_TOKEN, actorToken, actorTokenType, requestedType,
							audience != null ? Audience.create(audience) : null),
					(scopes != null ? new Scope(scopes.toArray(String[]::new)) : null), null, resources, null, null);
		}
	}

	private ExchangeReqBuilder exchange(AccessToken t)
	{
		return new ExchangeReqBuilder(t);
	}

	private HTTPResponse exec(TokenRequest req) throws Exception
	{
		HTTPRequest bare = req.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		return wrapped.send();
	}

	private void assertBadRequestWithInvalidScope(TokenRequest req) throws Exception
	{
		assertThat(exec(req).getStatusCode()).isEqualTo(HTTPResponse.SC_BAD_REQUEST);
		assertThat(exec(req).getBodyAsJSONObject().get("error")).isEqualTo(OAuth2Error.INVALID_SCOPE_CODE);

	}

	private void assertBadRequestWithInvalidParams(TokenRequest req) throws Exception
	{
		assertThat(exec(req).getStatusCode()).isEqualTo(HTTPResponse.SC_BAD_REQUEST);
		assertThat(exec(req).getBodyAsJSONObject().get("error")).isEqualTo(OAuth2Error.INVALID_REQUEST_CODE);

	}

}
