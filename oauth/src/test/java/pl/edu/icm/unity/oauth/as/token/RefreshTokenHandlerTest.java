/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthToken;

@RunWith(MockitoJUnitRunner.class)
public class RefreshTokenHandlerTest
{
	@Mock
	private OAuthRefreshTokenRepository refreshTokensDAO;
	@Mock
	private OAuthAccessTokenRepository accessTokensDAO;
	@Mock
	private OAuthClientTokensCleaner tokenCleaner;
	@Mock
	private TokenUtils tokenUtils;

	@Test
	public void shouldReturnErrorWhenTokenUsedAgain() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensDAO, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)).thenReturn(true);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		token.setContents(new OAuthToken().getSerialized());

		when(refreshTokensDAO.getUsedRefreshToken("usedRef")).thenReturn(Optional.of(token));
		Response resp = refreshTokenHandler.handleRefreshTokenGrant("usedRef", "scope", "");
		assertThat(resp.getStatus(), is(400));
	}

	@Test
	public void shouldClearAllTokensWhenRefreshTokenIsReused() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensDAO, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)).thenReturn(true);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientId(999l);
		token.setContents(oAuthToken.getSerialized());
		when(refreshTokensDAO.getUsedRefreshToken("usedRef")).thenReturn(Optional.of(token));
		refreshTokenHandler.handleRefreshTokenGrant("usedRef", "scope", "");
		verify(tokenCleaner).removeTokensForClient(999l, 1, "rolling");
	}

	@Test
	public void shouldNotRotateTokensForConfidentialClientAndFeatureEnabledPublic() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensDAO, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)).thenReturn(true);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientId(999l);
		oAuthToken.setClientUsername("client");
		oAuthToken.setClientType(ClientType.CONFIDENTIAL);
		oAuthToken.setRequestedScope(new String[]
		{ "scope1" });
		oAuthToken.setEffectiveScope(new String[]
		{ "scope1" });
		oAuthToken.setTokenValidity(1000);

		token.setContents(oAuthToken.getSerialized());
		when(refreshTokensDAO.getUsedRefreshToken("ref")).thenReturn(Optional.empty());
		when(refreshTokensDAO.readRefreshToken("ref")).thenReturn(token);

		OAuthToken oAuthToken2 = new OAuthToken(oAuthToken);

		when(tokenUtils.prepareNewToken(any(), any(), any(), anyLong(), anyLong(), any(), anyBoolean(), any()))
				.thenReturn(oAuthToken2);
		when(tokenUtils.getAccessTokenResponse(any(), any(), any(), any()))
				.thenReturn(new AccessTokenResponse(new Tokens(new BearerAccessToken(), null)));

		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 999l, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.setCurrent(context);

		Response resp = refreshTokenHandler.handleRefreshTokenGrant("ref", "scope1", "");
		ArgumentCaptor<RefreshToken> refreshToken = ArgumentCaptor.forClass(RefreshToken.class);

		verify(tokenUtils).getAccessTokenResponse(eq(oAuthToken2), any(), refreshToken.capture(), isNull());

		assertThat(refreshToken.getValue(), is(nullValue()));
		assertThat(resp.getStatus(), is(200));
	}

	@Test
	public void shouldNotRotateTokensWhenRollingNotEnabledAndConfidentialClient() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensDAO, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)).thenReturn(false);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientId(999l);
		oAuthToken.setClientUsername("client");
		oAuthToken.setClientType(ClientType.CONFIDENTIAL);
		oAuthToken.setRequestedScope(new String[]
		{ "scope1" });
		oAuthToken.setEffectiveScope(new String[]
		{ "scope1" });
		oAuthToken.setTokenValidity(1000);

		token.setContents(oAuthToken.getSerialized());
		when(refreshTokensDAO.readRefreshToken("ref")).thenReturn(token);

		OAuthToken oAuthToken2 = new OAuthToken(oAuthToken);

		when(tokenUtils.prepareNewToken(any(), any(), any(), anyLong(), anyLong(), any(), anyBoolean(), any()))
				.thenReturn(oAuthToken2);
		when(tokenUtils.getAccessTokenResponse(any(), any(), any(), any()))
				.thenReturn(new AccessTokenResponse(new Tokens(new BearerAccessToken(), null)));

		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 999l, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.setCurrent(context);

		Response resp = refreshTokenHandler.handleRefreshTokenGrant("ref", "scope1", "");
		ArgumentCaptor<RefreshToken> refreshToken = ArgumentCaptor.forClass(RefreshToken.class);

		verify(tokenUtils).getAccessTokenResponse(eq(oAuthToken2), any(), refreshToken.capture(), isNull());
		assertThat(refreshToken.getValue(), is(nullValue()));
		assertThat(resp.getStatus(), is(200));
	}

	@Test
	public void shouldNotRotateTokensWhenRollingNotEnabledAndPublicClient() throws JsonProcessingException, EngineException
	{
		OAuthASProperties config = mock(OAuthASProperties.class);
		AccessTokenFactory accessTokenFactory = new AccessTokenFactory(config);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokensDAO, accessTokenFactory,
				accessTokensDAO, tokenCleaner, tokenUtils);
		when(config.getBooleanValue(OAuthASProperties.ENABLE_REFRESH_TOKENS_FOR_PUBLIC_CLIENTS_WITH_ROTATION)).thenReturn(false);
		Token token = new Token(OAuthRefreshTokenRepository.INTERNAL_USED_REFRESH_TOKEN, "token", 1l);
		OAuthToken oAuthToken = new OAuthToken();
		oAuthToken.setFirstRefreshRollingToken("rolling");
		oAuthToken.setClientId(999l);
		oAuthToken.setClientUsername("client");
		oAuthToken.setClientType(ClientType.PUBLIC);
		oAuthToken.setRequestedScope(new String[]
		{ "scope1" });
		oAuthToken.setEffectiveScope(new String[]
		{ "scope1" });
		oAuthToken.setTokenValidity(1000);

		token.setContents(oAuthToken.getSerialized());
		when(refreshTokensDAO.readRefreshToken("ref")).thenReturn(token);

		OAuthToken oAuthToken2 = new OAuthToken(oAuthToken);

		when(tokenUtils.prepareNewToken(any(), any(), any(), anyLong(), anyLong(), any(), anyBoolean(), any()))
				.thenReturn(oAuthToken2);
		when(tokenUtils.getAccessTokenResponse(any(), any(), any(), any()))
				.thenReturn(new AccessTokenResponse(new Tokens(new BearerAccessToken(), null)));

		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 999l, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.setCurrent(context);

		Response resp = refreshTokenHandler.handleRefreshTokenGrant("ref", "scope1", "");
		ArgumentCaptor<RefreshToken> refreshToken = ArgumentCaptor.forClass(RefreshToken.class);

		verify(tokenUtils).getAccessTokenResponse(eq(oAuthToken2), any(), refreshToken.capture(), isNull());
		assertThat(refreshToken.getValue(), is(nullValue()));
		assertThat(resp.getStatus(), is(200));
	}

	
}
