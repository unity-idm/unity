/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.TestTxRunner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class DeviceCodeHandlerTest
{
	private TransactionalRunner tx = new TestTxRunner();
	private MockTokensMan tokensManagement;
	private DeviceCodeRepository deviceCodeRepository;
	private OAuthASProperties config;
	private DeviceCodeHandler tested;

	@BeforeEach
	void setUp()
	{
		tokensManagement = new MockTokensMan();
		config = OAuthTestUtils.getConfig();
		config.setProperty(OAuthASProperties.DEVICE_GRANT_ENABLED, "true");

		deviceCodeRepository = new DeviceCodeRepository(tokensManagement);

		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));
		TokenService tokenService = new TokenService(config, null);
		OAuthTokenStatisticPublisher publisher = new OAuthTokenStatisticPublisher(mock(ApplicationEventPublisher.class),
				null, null, null, null, mock(LastIdPClinetAccessAttributeManagement.class), null, config,
				OAuthTestUtils.getEndpoint());

		tested = new DeviceCodeHandler(deviceCodeRepository, tx, new AccessTokenFactory(config), accessTokenRepository,
				refreshTokenRepository, publisher, config, tokenService);

		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);
		InvocationContext.setCurrent(new InvocationContext(null, realm, Collections.emptyList()));
	}

	private OAuthToken buildApprovedToken()
	{
		OAuthToken token = new OAuthToken();
		token.setClientId(100);
		token.setClientUsername("clientC");
		token.setClientType(ClientType.PUBLIC);
		token.setEffectiveScope(List.of());
		token.setIssuerUri(OAuthTestUtils.ISSUER);
		token.setSubject("userA");
		token.setUserInfo(new UserInfo(new Subject("userA")).toJSONObject().toJSONString());
		token.setAuthenticationTime(Instant.now());
		token.setSubjectEntityId(7L);
		token.setTokenValidity(100);
		token.setAudience(List.of("clientC"));
		token.setDeviceCodeStatus(DeviceCodeStatus.APPROVED);
		return token;
	}

	@Test
	void shouldReturnAuthorizationPendingForPendingCode() throws Exception
	{
		OAuthToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", null);

		assertEquals(400, resp.getStatus());
		assertEquals("authorization_pending", getError(resp));
	}

	@Test
	void shouldReturnSlowDownOnRepeatedFastPoll() throws Exception
	{
		OAuthToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		tested.handleDeviceCodeGrant("dc1", null);
		Response resp = tested.handleDeviceCodeGrant("dc1", null);

		assertEquals(400, resp.getStatus());
		assertEquals("slow_down", getError(resp));

		Token updated = tokensManagement.getTokenById(DeviceCodeRepository.INTERNAL_DEVICE_TOKEN, "dc1");
		OAuthToken updatedToken = OAuthToken.getInstanceFromJson(updated.getContents());
		assertEquals(OAuthASProperties.DEFAULT_DEVICE_CODE_MIN_POLL_INTERVAL + 5, updatedToken.getCurrentPollInterval());
	}

	@Test
	void shouldReturnAccessDeniedAndRemoveRecordWhenDenied() throws Exception
	{
		OAuthToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", null);

		assertEquals(403, resp.getStatus());
		assertEquals("access_denied", getError(resp));
		assertTrue(deviceCodeRepository.getByDeviceCode("dc1").isEmpty());
	}

	@Test
	void shouldReturnExpiredTokenAndRemoveRecordWhenExpired() throws Exception
	{
		OAuthToken token = buildApprovedToken();
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date past = new Date(System.currentTimeMillis() - 10_000);
		deviceCodeRepository.store("dc1", token, new Date(System.currentTimeMillis() - 20_000), past);

		Response resp = tested.handleDeviceCodeGrant("dc1", null);

		assertEquals(400, resp.getStatus());
		assertEquals("expired_token", getError(resp));
		assertTrue(deviceCodeRepository.getByDeviceCode("dc1").isEmpty());
	}

	@Test
	void shouldReturnInvalidGrantForUnknownCode() throws Exception
	{
		Response resp = tested.handleDeviceCodeGrant("missing", null);

		assertEquals(400, resp.getStatus());
		assertEquals("invalid_grant", getError(resp));
	}

	@Test
	void shouldIssueAccessTokenAndRemoveRecordOnApprovedThenRejectSecondPoll() throws Exception
	{
		OAuthToken token = buildApprovedToken();
		Date now = new Date();
		deviceCodeRepository.store("dc1", token, now, new Date(now.getTime() + 60_000));

		Response resp = tested.handleDeviceCodeGrant("dc1", null);

		assertEquals(200, resp.getStatus());
		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setBody(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		AccessTokenResponse parsed = AccessTokenResponse.parse(httpResp);
		assertThat(parsed.getTokens().getAccessToken()).isNotNull();

		assertTrue(deviceCodeRepository.getByDeviceCode("dc1").isEmpty());

		Response resp2 = tested.handleDeviceCodeGrant("dc1", null);
		assertEquals(400, resp2.getStatus());
		assertEquals("invalid_grant", getError(resp2));
	}

	private static Object getError(Response resp)
	{
		JSONObject body = (JSONObject) JSONValue.parse(resp.getEntity().toString());
		return body.get("error");
	}
}
