/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.Tokens;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.oauth.as.ActiveOAuthScopeDefinition;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.RequestedOAuthScope;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenHandlerTest
{
	@Mock
	private OAuthRefreshTokenRepository refreshTokensRepository;
	@Mock
	private OAuthAccessTokenRepository accessTokensRepository;
	@Mock
	private OAuthClientTokensCleaner tokenCleaner;
	@Mock
	private TokenService tokenService;
	@Mock
	private EffectiveScopesAttributesCompleter oAuthTokenEffectiveScopesAttributesCompleter; 

	
	@Test
	public void shouldReturnErrorWhenTokenUsedAgain() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensRepository,
				accessTokenFactory, accessTokensRepository, tokenCleaner, tokenService, oAuthTokenEffectiveScopesAttributesCompleter);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
				.thenReturn(true);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		token.setContents(new OAuthToken().getSerialized());

		when(refreshTokensRepository.getUsedRefreshToken("usedRef")).thenReturn(Optional.of(token));
		Response resp = refreshTokenHandler.handleRefreshTokenGrant("usedRef", "scope", "");
		assertThat(resp.getStatus()).isEqualTo(400);
	}

	@Test
	public void shouldClearAllTokensWhenRefreshTokenIsReused() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensRepository,
				accessTokenFactory, accessTokensRepository, tokenCleaner, tokenService, oAuthTokenEffectiveScopesAttributesCompleter);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION))
				.thenReturn(true);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientId(999l);
		token.setContents(oAuthToken.getSerialized());
		when(refreshTokensRepository.getUsedRefreshToken("usedRef")).thenReturn(Optional.of(token));
		refreshTokenHandler.handleRefreshTokenGrant("usedRef", "scope", "");
		verify(tokenCleaner).removeTokensForClient(999l, 1, "rolling");
	}

	@Test
	public void shouldReturnErrorWhenConfidentialClientIsNotAuthenticate()
			throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensRepository,
				accessTokenFactory, accessTokensRepository, tokenCleaner, tokenService, oAuthTokenEffectiveScopesAttributesCompleter);

		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientType(ClientType.CONFIDENTIAL);
		oAuthToken.setClientId(999l);
		token.setContents(oAuthToken.getSerialized());

		when(refreshTokensRepository.readRefreshToken("token")).thenReturn(token);
		InvocationContext context = new InvocationContext(null, null, null);
		InvocationContext.setCurrent(context);

		Response response = refreshTokenHandler.handleRefreshTokenGrant("token", "scope", "");

		assertEquals(HTTPResponse.SC_UNAUTHORIZED, response.getStatus());
	}

	@Test
	public void shouldProcessingRefreshTokenWhenPublicClientIsNotAuthenticate() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensRepository,
				accessTokenFactory, accessTokensRepository, tokenCleaner, tokenService, oAuthTokenEffectiveScopesAttributesCompleter);

		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientType(ClientType.PUBLIC);
		oAuthToken.setClientId(999l);
		oAuthToken.setClientUsername("client");
		oAuthToken.setRequestedScope(new String[]
		{ "scope" });
		oAuthToken.setEffectiveScope(List.of(new RequestedOAuthScope("scope", ActiveOAuthScopeDefinition.builder().withName("scope").build(), false)));
		oAuthToken.setTokenValidity(1000);
		token.setContents(oAuthToken.getSerialized());
		when(refreshTokensRepository.readRefreshToken("token")).thenReturn(token);
		when(tokenService.prepareTokenForRefresh(any(OAuthToken.class), any(Scope.class), anyList(), anyLong(),
				anyLong(), anyList(), eq(true), anyString())).thenReturn(oAuthToken);
		when(tokenService.getAccessTokenResponse(any(OAuthToken.class), any(AccessToken.class), eq(null), eq(null)))
				.thenReturn(new AccessTokenResponse(new Tokens(new BearerAccessToken(), null)));
		Response response = refreshTokenHandler.handleRefreshTokenGrant("token", "scope", "");
		assertEquals(HTTPResponse.SC_OK, response.getStatus());
	}
}
