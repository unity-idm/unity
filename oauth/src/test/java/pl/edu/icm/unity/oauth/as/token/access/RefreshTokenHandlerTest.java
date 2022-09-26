/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;

@RunWith(MockitoJUnitRunner.class)
public class RefreshTokenHandlerTest
{
	@Mock
	private OAuthRefreshTokenRepository refreshTokensRepository;
	@Mock
	private OAuthAccessTokenRepository accessTokensDAO;
	@Mock
	private OAuthClientTokensCleaner tokenCleaner;
	@Mock
	private TokenService tokenUtils;

	@Test
	public void shouldReturnErrorWhenTokenUsedAgain() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensRepository, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)).thenReturn(true);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		token.setContents(new OAuthToken().getSerialized());

		when(refreshTokensRepository.getUsedRefreshToken("usedRef")).thenReturn(Optional.of(token));
		Response resp = refreshTokenHandler.handleRefreshTokenGrant("usedRef", "scope", "");
		assertThat(resp.getStatus(), is(400));
	}

	@Test
	public void shouldClearAllTokensWhenRefreshTokenIsReused() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensRepository, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)).thenReturn(true);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientId(999l);
		token.setContents(oAuthToken.getSerialized());
		when(refreshTokensRepository.getUsedRefreshToken("usedRef")).thenReturn(Optional.of(token));
		refreshTokenHandler.handleRefreshTokenGrant("usedRef", "scope", "");
		verify(tokenCleaner).removeTokensForClient(999l, 1, "rolling");
	}
	
}
