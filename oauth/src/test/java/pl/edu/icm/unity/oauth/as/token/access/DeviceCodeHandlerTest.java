/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.RollbackOnThrowTxRunner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class DeviceCodeHandlerTest
{
	// unlike a plain TestTxRunner, this rolls back token store writes made by code that
	// throws instead of returning normally - matching the real SQLTransactionEngine, whose
	// commit only runs on normal return. Without this, tests can't tell apart a handler that
	// persists-then-throws (broken: production never commits) from one that persists-then-returns.
	private TransactionalRunner tx;
	private MockTokensMan tokensManagement;
	private DeviceCodeRepository deviceCodeRepository;
	private OAuthASProperties config;
	private OAuthAccessTokenRepository accessTokenRepository;
	private OAuthRefreshTokenRepository refreshTokenRepository;
	private TokenService tokenService;
	private DeviceCodeHandler tested;

	@BeforeEach
	void setUp()
	{
		tokensManagement = new MockTokensMan();
		tx = new RollbackOnThrowTxRunner(tokensManagement);
		config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.DEVICE_GRANT_ENABLED, "true");

		deviceCodeRepository = new DeviceCodeRepository(tokensManagement);

		accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement, mock(SecuredTokensManagement.class));
		refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement, mock(SecuredTokensManagement.class));
		tokenService = new TokenService(config, null);
		OAuthTokenStatisticPublisher publisher = new OAuthTokenStatisticPublisher(mock(ApplicationEventPublisher.class),
				null, null, null, null, mock(LastIdPClinetAccessAttributeManagement.class), null, config,
				OAuthTestUtils.getEndpoint());

		tested = new DeviceCodeHandler(deviceCodeRepository, tx, new AccessTokenFactory(config), accessTokenRepository,
				refreshTokenRepository, publisher, config, tokenService);

		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);
		InvocationContext.setCurrent(new InvocationContext(null, realm, Collections.emptyList()));
	}

	private DeviceCodeToken buildApprovedToken()
	{
		OAuthToken oauthToken = new OAuthToken();
		oauthToken.setClientId(100);
		oauthToken.setClientUsername("clientC");
		oauthToken.setClientType(ClientType.PUBLIC);
		oauthToken.setEffectiveScope(List.of());
		oauthToken.setRequestedScope(new String[0]);
		oauthToken.setIssuerUri(OAuthTestUtils.ISSUER);
		oauthToken.setSubject("userA");
		oauthToken.setUserInfo(new UserInfo(new Subject("userA")).toJSONObject().toJSONString());
		oauthToken.setAuthenticationTime(Instant.now());
		oauthToken.setTokenValidity(100);
		oauthToken.setAudience(List.of("clientC"));

		DeviceCodeToken token = new DeviceCodeToken();
		token.setOauthToken(oauthToken);
		token.setSubjectEntityId(7L);
		token.setDeviceCodeStatus(DeviceCodeStatus.APPROVED);
		return token;
	}

	@Test
	void shouldRejectConfidentialClientPollingWithoutAuthentication() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.getOauthToken().setClientType(ClientType.CONFIDENTIAL);
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", null, null);

		assertEquals(401, resp.getStatus());
		assertEquals("invalid_client", getError(resp));
	}

	@Test
	void shouldRejectPublicClientPollingWithoutClientId() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", null, null);

		assertEquals(400, resp.getStatus());
		assertEquals("invalid_grant", getError(resp));
	}

	@Test
	void shouldRejectPublicClientPollingWithWrongClientId() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", "otherClient", null);

		assertEquals(400, resp.getStatus());
		assertEquals("invalid_grant", getError(resp));
	}

	@Test
	void shouldRejectCodeIssuedForAnotherIssuer() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.getOauthToken().setIssuerUri("https://other.issuer.example.com/token");
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(400, resp.getStatus());
		assertEquals("invalid_grant", getError(resp));
		// the record must be left untouched - it still belongs to its real issuing endpoint
		assertTrue(deviceCodeRepository.getByDeviceCode("dc1").isPresent());
	}

	@Test
	void shouldRejectPollWhenDeviceGrantDisabledOnThisEndpoint() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		config.setProperty(OAuthASProperties.DEVICE_GRANT_ENABLED, "false");

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(400, resp.getStatus());
		assertEquals("invalid_request", getError(resp));
	}

	@Test
	void shouldReturnAuthorizationPendingForPendingCode() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(400, resp.getStatus());
		assertEquals("authorization_pending", getError(resp));
	}

	@Test
	void shouldReturnSlowDownOnRepeatedFastPoll() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		tested.handleDeviceCodeGrant("dc1", "clientC", null);
		Token afterFirstPoll = tokensManagement.getTokenById(DeviceCodeRepository.INTERNAL_DEVICE_TOKEN, "dc1");
		DeviceCodeToken tokenAfterFirstPoll = DeviceCodeToken.getInstanceFromJson(afterFirstPoll.getContents());

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(400, resp.getStatus());
		assertEquals("slow_down", getError(resp));

		// a rejected (too-fast) poll must not write anything - otherwise a client hammering the
		// endpoint could turn every rejected request into a DB write (a DoS amplifier). The record
		// after the rejected second poll must be byte-for-byte the same as after the first one.
		Token afterSecondPoll = tokensManagement.getTokenById(DeviceCodeRepository.INTERNAL_DEVICE_TOKEN, "dc1");
		DeviceCodeToken tokenAfterSecondPoll = DeviceCodeToken.getInstanceFromJson(afterSecondPoll.getContents());
		assertEquals(tokenAfterFirstPoll.getLastPolledAt(), tokenAfterSecondPoll.getLastPolledAt());
		assertEquals(tokenAfterFirstPoll.getCurrentPollInterval(), tokenAfterSecondPoll.getCurrentPollInterval());
	}

	@Test
	void shouldAcceptPollAgainOncePollIntervalHasElapsed() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		token.setCurrentPollInterval(1);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		tested.handleDeviceCodeGrant("dc1", "clientC", null);
		Thread.sleep(1100);
		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(400, resp.getStatus());
		assertEquals("authorization_pending", getError(resp));
	}

	@Test
	void shouldReturnAccessDeniedAndRemoveRecordWhenDenied() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		// RFC 6749 §5.2: token endpoint errors use 400, not the 403 that OAuth2Error.ACCESS_DENIED
		// defaults to for the authorization endpoint's redirect-based error
		assertEquals(400, resp.getStatus());
		assertEquals("access_denied", getError(resp));
		assertTrue(deviceCodeRepository.getByDeviceCode("dc1").isEmpty());
	}

	@Test
	void shouldReturnExpiredTokenAndRemoveRecordWhenExpired() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date past = new Date(System.currentTimeMillis() - 10_000);
		deviceCodeRepository.store("dc1", token, new Date(System.currentTimeMillis() - 20_000), past);

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(400, resp.getStatus());
		assertEquals("expired_token", getError(resp));
		assertTrue(deviceCodeRepository.getByDeviceCode("dc1").isEmpty());
	}

	@Test
	void shouldReturnInvalidGrantForUnknownCode() throws Exception
	{
		Response resp = tested.handleDeviceCodeGrant("missing", "clientC", null);

		assertEquals(400, resp.getStatus());
		assertEquals("invalid_grant", getError(resp));
	}

	@Test
	void shouldIssueAccessTokenAndRemoveRecordOnApprovedThenRejectSecondPoll() throws Exception
	{
		DeviceCodeToken token = buildApprovedToken();
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(200, resp.getStatus());
		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setBody(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		AccessTokenResponse parsed = AccessTokenResponse.parse(httpResp);
		assertThat(parsed.getTokens().getAccessToken()).isNotNull();

		assertTrue(deviceCodeRepository.getByDeviceCode("dc1").isEmpty());

		Response resp2 = tested.handleDeviceCodeGrant("dc1", "clientC", null);
		assertEquals(400, resp2.getStatus());
		assertEquals("invalid_grant", getError(resp2));
	}

	@Test
	void shouldIncludeIdTokenInResponseWhenOpenidScopeWasApproved() throws Exception
	{
		// reproduces the QA report "no id_token for oidc": exercises the read/attach half of the
		// chain (DeviceCodeHandler + the shared TokenService), complementing
		// DeviceCodeTransitionServiceTest which covers the mint/persist half
		DeviceCodeToken token = buildApprovedToken();
		token.getOauthToken()
				.setEffectiveScope(List.of(new RequestedOAuthScope(OAuthSystemScopeProvider.OPENID_SCOPE,
						ActiveOAuthScopeDefinition.builder()
								.withName(OAuthSystemScopeProvider.OPENID_SCOPE)
								.withDescription("openid")
								.build(),
						false)));
		Date now = new Date();
		IDTokenClaimsSet idTokenClaims = new IDTokenClaimsSet(new Issuer(OAuthTestUtils.ISSUER), new Subject("userA"),
				List.of(new Audience("clientC")), new Date(now.getTime() + 300_000), now);
		// signed with a separately-initialized OIDC config: DeviceCodeHandler never signs an id
		// token itself (that happens earlier, in DeviceCodeTransitionService at consent time), it
		// only decodes whatever is already stored in openidInfo - this mirrors that division
		token.getOauthToken()
				.setOpenidToken(OAuthTestUtils.getOIDCConfig().getTokenSigner().sign(idTokenClaims).serialize());
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", "clientC", null);

		assertEquals(200, resp.getStatus());
		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setBody(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse parsed = OIDCTokenResponse.parse(httpResp);
		assertThat(parsed.getOIDCTokens().getAccessToken()).isNotNull();
		assertThat(parsed.getOIDCTokens().getIDToken()).isNotNull();
		assertEquals("userA", parsed.getOIDCTokens().getIDToken().getJWTClaimsSet().getSubject());
	}

	@Test
	void shouldIncludeRefreshTokenWhenOfflineAccessScopeWasApproved() throws Exception
	{
		// exercises the default REFRESH_TOKEN_ISSUE_POLICY (OFFLINE_SCOPE_BASED): a refresh_token
		// must be issued when the client requested and was granted offline_access. Confidential
		// client on purpose - a PUBLIC client never gets a refresh token here regardless of scope
		// unless issueRefreshTokensWithRotationForPublicClients is on (a separate, unrelated gate)
		DeviceCodeToken token = buildApprovedToken();
		token.getOauthToken().setClientType(ClientType.CONFIDENTIAL);
		token.getOauthToken()
				.setEffectiveScope(List.of(new RequestedOAuthScope(OAuthSystemScopeProvider.OFFLINE_ACCESS_SCOPE,
						ActiveOAuthScopeDefinition.builder()
								.withName(OAuthSystemScopeProvider.OFFLINE_ACCESS_SCOPE)
								.withDescription("offline_access")
								.build(),
						false)));
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));
		authenticateAsClient(now);

		Response resp = tested.handleDeviceCodeGrant("dc1", null, null);

		assertEquals(200, resp.getStatus());
		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setBody(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		AccessTokenResponse parsed = AccessTokenResponse.parse(httpResp);
		assertThat(parsed.getTokens().getRefreshToken()).isNotNull();
	}

	@Test
	void shouldNotIncludeRefreshTokenWhenOfflineAccessScopeWasNotRequested() throws Exception
	{
		// same setup as shouldIncludeRefreshTokenWhenOfflineAccessScopeWasApproved, minus the
		// offline_access scope (buildApprovedToken's effectiveScope is already empty) - the negative
		// side of the same OFFLINE_SCOPE_BASED policy check
		DeviceCodeToken token = buildApprovedToken();
		token.getOauthToken().setClientType(ClientType.CONFIDENTIAL);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));
		authenticateAsClient(now);

		Response resp = tested.handleDeviceCodeGrant("dc1", null, null);

		assertEquals(200, resp.getStatus());
		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setBody(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		AccessTokenResponse parsed = AccessTokenResponse.parse(httpResp);
		assertThat(parsed.getTokens().getRefreshToken()).isNull();
	}

	private void authenticateAsClient(Date now)
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);
		InvocationContext authed = new InvocationContext(null, realm, Collections.emptyList());
		LoginSession loginSession = new LoginSession("sid", now, 1000, 100L, "clientC", null, null, null);
		authed.setLoginSession(loginSession);
		InvocationContext.setCurrent(authed);
	}

	@Test
	void shouldRedeemRefreshTokenIssuedByDeviceFlowWithoutError() throws Exception
	{
		// reproduces UY-1594 P1: DeviceAuthorizationResource used to never initialize
		// requestedScope, so the first refresh of a device-flow-issued token NPE'd in
		// RefreshTokenHandler (Arrays.asList(null)).
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, RefreshTokenIssuePolicy.ALWAYS.toString());

		DeviceCodeToken token = buildApprovedToken();
		token.getOauthToken().setClientType(ClientType.CONFIDENTIAL);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);
		InvocationContext authed = new InvocationContext(null, realm, Collections.emptyList());
		LoginSession loginSession = new LoginSession("sid", now, 1000, 100L, "clientC", null, null, null);
		authed.setLoginSession(loginSession);
		InvocationContext.setCurrent(authed);

		Response tokenResp = tested.handleDeviceCodeGrant("dc1", null, null);
		assertEquals(200, tokenResp.getStatus());
		HTTPResponse httpResp = new HTTPResponse(tokenResp.getStatus());
		httpResp.setBody(tokenResp.getEntity().toString());
		httpResp.setContentType("application/json");
		AccessTokenResponse parsedTokenResp = AccessTokenResponse.parse(httpResp);
		String refreshTokenValue = parsedTokenResp.getTokens().getRefreshToken().getValue();
		assertThat(refreshTokenValue).isNotNull();

		// mocked out: exercising the real TokenService here would require a full IdP attribute
		// resolution stack unrelated to this bug; the NPE this test guards against happens earlier,
		// at Arrays.asList(parsedRefreshToken.getRequestedScope()) in RefreshTokenHandler, before
		// TokenService is ever invoked
		TokenService mockedTokenService = mock(TokenService.class);
		when(mockedTokenService.prepareTokenForRefresh(any(OAuthToken.class), any(Scope.class), anyList(), anyLong(),
				anyLong(), anyList(), eq(true), anyString())).thenAnswer(inv -> inv.getArgument(0));
		when(mockedTokenService.getAccessTokenResponse(any(OAuthToken.class), any(AccessToken.class), any(), any()))
				.thenReturn(new AccessTokenResponse(new Tokens(new BearerAccessToken(), null)));
		RefreshTokenHandler refreshHandler = new RefreshTokenHandler(config, refreshTokenRepository,
				new AccessTokenFactory(config), accessTokenRepository, mock(OAuthClientTokensCleaner.class),
				mockedTokenService, mock(EffectiveScopesAttributesCompleter.class));

		Response refreshResp = refreshHandler.handleRefreshTokenGrant(refreshTokenValue, null, null);

		assertEquals(200, refreshResp.getStatus());
	}

	private static Object getError(Response resp)
	{
		JSONObject body = (JSONObject) JSONValue.parse(resp.getEntity().toString());
		return body.get("error");
	}
}
