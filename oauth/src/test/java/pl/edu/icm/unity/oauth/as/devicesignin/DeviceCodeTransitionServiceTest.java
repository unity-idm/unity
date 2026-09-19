/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthSystemScopeProvider;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;
import pl.edu.icm.unity.oauth.as.TestTxRunner;
import pl.edu.icm.unity.oauth.as.devicesignin.DeviceCodeTransitionService.TransitionResult;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Regression coverage for the QA report "no id_token for the device flow with the openid scope":
 * exercises the mint/persist half of the chain ({@link DeviceCodeTransitionService#approve}), which
 * {@link pl.edu.icm.unity.oauth.as.token.access.DeviceCodeHandlerTest} complements by covering the
 * read/attach half.
 */
class DeviceCodeTransitionServiceTest
{
	private DeviceCodeRepository deviceCodeRepository;
	private DeviceCodeTransitionService tested;

	@BeforeEach
	void setUp()
	{
		MockTokensMan tokensManagement = new MockTokensMan();
		deviceCodeRepository = new DeviceCodeRepository(tokensManagement);
		tested = new DeviceCodeTransitionService(new TestTxRunner(), deviceCodeRepository);
	}

	private void storePending(String deviceCode, OAuthToken oauthToken) throws Exception
	{
		DeviceCodeToken token = new DeviceCodeToken();
		token.setOauthToken(oauthToken);
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		Date now = new Date();
		deviceCodeRepository.store(deviceCode, token, now, new Date(now.getTime() + 60_000));
	}

	private DeviceCodeToken reload(String deviceCode)
	{
		Token stored = deviceCodeRepository.getByDeviceCode(deviceCode).orElseThrow();
		return DeviceCodeToken.getInstanceFromJson(stored.getContents());
	}

	@Test
	void shouldSignAndRecordIdTokenWhenOpenidScopeWasRequested() throws Exception
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		OAuthToken oauthToken = new OAuthToken();
		oauthToken.setClientUsername("clientC");
		oauthToken.setEffectiveScope(List.of(new RequestedOAuthScope(OAuthSystemScopeProvider.OPENID_SCOPE,
				ActiveOAuthScopeDefinition.builder()
						.withName(OAuthSystemScopeProvider.OPENID_SCOPE)
						.withDescription("openid")
						.build(),
				false)));
		storePending("dc1", oauthToken);

		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		UserInfo userInfo = new UserInfo(new Subject("userA"));

		TransitionResult result = tested.approve("dc1", identity, userInfo, null, 7L, Instant.now(), config);

		assertThat(result).isEqualTo(TransitionResult.APPLIED);
		DeviceCodeToken stored = reload("dc1");
		assertThat(stored.getDeviceCodeStatus()).isEqualTo(DeviceCodeStatus.APPROVED);
		assertThat(stored.getOauthToken().getOpenidInfo()).isNotNull();

		SignedJWT idToken = SignedJWT.parse(stored.getOauthToken().getOpenidInfo());
		assertThat(idToken.getJWTClaimsSet().getSubject()).isEqualTo("userA");
		assertThat(idToken.getJWTClaimsSet().getIssuer()).isEqualTo(OAuthTestUtils.ISSUER);
		assertThat(idToken.getJWTClaimsSet().getAudience()).contains("clientC");
	}

	@Test
	void shouldNotRecordIdTokenWhenOpenidScopeWasNotRequested() throws Exception
	{
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		OAuthToken oauthToken = new OAuthToken();
		oauthToken.setClientUsername("clientC");
		oauthToken.setEffectiveScope(List.of());
		storePending("dc2", oauthToken);

		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		UserInfo userInfo = new UserInfo(new Subject("userA"));

		TransitionResult result = tested.approve("dc2", identity, userInfo, null, 7L, Instant.now(), config);

		assertThat(result).isEqualTo(TransitionResult.APPLIED);
		assertThat(reload("dc2").getOauthToken().getOpenidInfo()).isNull();
	}
}
