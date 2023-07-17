/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthToken;

public class OAuthRefreshTokenRepositoryTest
{
	@Test
	public void shouldRemoveHistoryWhenRemovingRefreshToken()
			throws JsonProcessingException, IllegalTypeException, IllegalIdentityValueException
	{
		TokensManagement tokensManagement = new MockTokensMan();

		OAuthToken token = new OAuthToken();
		token.setRefreshToken("ref");
		token.setFirstRefreshRollingToken("ref");
		token.setClientId(99l);
		tokensManagement.addToken(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN, "ref", new EntityParam(0l),
				token.getSerialized(), null, null);

		OAuthToken htoken = new OAuthToken();
		htoken.setRefreshToken("ref_his");
		htoken.setFirstRefreshRollingToken("ref");
		htoken.setClientId(99l);
		tokensManagement.addToken(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "ref_his",
				new EntityParam(0l), htoken.getSerialized(), null, null);

		OAuthRefreshTokenRepository rep = new OAuthRefreshTokenRepository(tokensManagement, null);
		rep.removeRefreshToken("ref", token, 0);

		assertThat(tokensManagement.getAllTokens(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN)).isEmpty();
		assertThat(tokensManagement.getAllTokens(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN)).isEmpty();
	}

	@Test
	public void shouldNotRotateTokensForConfidentialClientAndFeatureEnabled()
			throws JsonProcessingException, EngineException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthRefreshTokenRepository rep = new OAuthRefreshTokenRepository(tokensManagement, null);

		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
				.thenReturn(true);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setClientType(ClientType.CONFIDENTIAL);

		Optional<RefreshToken> refreshToken = rep.rotateRefreshTokenIfNeeded(config, null, oAuthToken, null, null);

		assertThat(refreshToken.isEmpty()).isTrue();
	}

	@Test
	public void shouldNotRotateTokensForConfidentialClientAndFeatureDisabled()
			throws JsonProcessingException, EngineException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthRefreshTokenRepository rep = new OAuthRefreshTokenRepository(tokensManagement, null);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
				.thenReturn(false);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setClientType(ClientType.CONFIDENTIAL);

		Optional<RefreshToken> refreshToken = rep.rotateRefreshTokenIfNeeded(config, null, oAuthToken, null, null);

		assertThat(refreshToken.isEmpty()).isTrue();
	}

	@Test
	public void shouldNotRotateTokensForPublicClientAndFeatureDisabled() throws JsonProcessingException, EngineException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthRefreshTokenRepository rep = new OAuthRefreshTokenRepository(tokensManagement, null);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
				.thenReturn(false);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setClientType(ClientType.PUBLIC);

		Optional<RefreshToken> refreshToken = rep.rotateRefreshTokenIfNeeded(config, null, oAuthToken, null, null);

		assertThat(refreshToken.isEmpty()).isTrue();
	}

	@Test
	public void shouldRotateTokensForPublicClientAndFeatureEnbaled() throws JsonProcessingException, EngineException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthRefreshTokenRepository rep = new OAuthRefreshTokenRepository(tokensManagement, null);
		OAuthASProperties config = mock(OAuthASProperties.class);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
				.thenReturn(true);
		when(config.getRefreshTokenIssuePolicy()).thenReturn(RefreshTokenIssuePolicy.ALWAYS);

		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setClientType(ClientType.PUBLIC);
		oAuthToken.setEffectiveScope(new String[]
		{ "scope1" });
		OAuthToken oldRefresh = new OAuthToken();
		oAuthToken.setClientType(ClientType.PUBLIC);
		oldRefresh.setRefreshToken("ref");
		oldRefresh.setEffectiveScope(new String[]
		{ "scope1" });
		Optional<RefreshToken> refreshToken = rep.rotateRefreshTokenIfNeeded(config, new Date(), oAuthToken, oldRefresh,
				1L);
		assertThat(refreshToken.isEmpty()).isFalse();
	}
}
