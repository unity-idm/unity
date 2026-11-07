/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to4_3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.impl.tokens.TokenRDBMSStore;

/**
 * The 4.2 client credentials flow stores {@code "effectiveScope":null} when the request has no scope. Such a token
 * must not abort the 4.3 content migration of the remaining tokens (UY-1618).
 */
class InDBUpdateFromSchema21NullScopeTest
{
	private static final String CLIENT_CREDENTIALS_ACCESS_TOKEN_4_2 = "{\"userInfo\":\"{\\\"sub\\\":\\\"svc\\\"}\","
			+ "\"openidInfo\":null,\"authzCode\":null,\"accessToken\":null,\"refreshToken\":null,"
			+ "\"firstRefreshRollingToken\":null,\"effectiveScope\":null,\"requestedScope\":null,"
			+ "\"redirectUri\":null,\"subject\":\"svc\",\"clientName\":null,\"clientUsername\":\"svc\","
			+ "\"maxExtendedValidity\":0,\"tokenValidity\":3600,\"responseType\":null,\"audience\":[\"svc\"],"
			+ "\"issuerUri\":\"https://localhost:2443/oauth2\",\"clientType\":\"CONFIDENTIAL\","
			+ "\"pkcsInfo\":{\"codeChallenge\":null,\"codeChallengeMethod\":null},\"clientId\":3}";

	private static final String REFRESH_TOKEN_4_2 = "{\"effectiveScope\":[\"profile\"],"
			+ "\"requestedScope\":[\"profile\"],\"clientUsername\":\"app\",\"clientId\":4}";

	@Test
	void shouldConvertRefreshTokenWhenOtherTokenHasNullEffectiveScope() throws IOException
	{
		TokenRDBMSStore tokensDAO = mock(TokenRDBMSStore.class);
		Token accessToken = token("oauth2Access", "a1", CLIENT_CREDENTIALS_ACCESS_TOKEN_4_2);
		Token refreshToken = token("oauth2Refresh", "r1", REFRESH_TOKEN_4_2);
		when(tokensDAO.getAll()).thenReturn(List.of(accessToken, refreshToken));
		InDBUpdateFromSchema21 updater = new InDBUpdateFromSchema21(tokensDAO);

		updater.updateOAuthTokens();

		ArgumentCaptor<Token> updated = ArgumentCaptor.forClass(Token.class);
		verify(tokensDAO, times(1)).update(updated.capture());
		assertThat(updated.getValue().getValue()).isEqualTo("r1");
		JsonNode scope = JsonUtil.parse(updated.getValue().getContents()).get("effectiveScope").get(0);
		assertThat(scope.get("scope").asText()).isEqualTo("profile");
		assertThat(scope.get("scopeDefinition").get("name").asText()).isEqualTo("profile");
	}

	private static Token token(String type, String value, String contents)
	{
		Token token = new Token(type, value, 1L);
		token.setContents(contents.getBytes(StandardCharsets.UTF_8));
		return token;
	}
}
