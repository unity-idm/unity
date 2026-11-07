/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.tokens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.api.TokenDAO;

class TokensIETest
{
	@Test
	void shouldExportPre43OAuthTokenWithEffectiveScopeInCurrentFormat() throws IOException
	{
		Token refreshToken = token("oauth2Refresh", "{\"effectiveScope\":[\"openid\"],\"clientUsername\":\"app\"}");

		JsonNode exported = export(refreshToken);

		JsonNode contents = exportedContents(exported.get(0));
		assertThat(contents.get("clientUsername").asText()).isEqualTo("app");
		assertThat(contents.get("effectiveScope")).isEqualTo(JsonUtil.parse("{\"s\":[{\"scope\":\"openid\","
				+ "\"scopeDefinition\":{\"name\":\"openid\",\"description\":\"\",\"pattern\":false,"
				+ "\"attributes\":null},\"pattern\":false}]}").get("s"));
	}

	@Test
	void shouldExportOtherTokensUnchanged() throws IOException
	{
		String nullScope = "{\"effectiveScope\":null,\"clientUsername\":\"svc\"}";
		String session = "{\"realm\":\"main\"}";

		JsonNode exported = export(token("oauth2Access", nullScope), token("session", session));

		assertThat(exportedContents(exported.get(0))).isEqualTo(JsonUtil.parse(nullScope));
		assertThat(exportedContents(exported.get(1))).isEqualTo(JsonUtil.parse(session));
	}

	private static JsonNode export(Token... tokens) throws IOException
	{
		TokenDAO dao = mock(TokenDAO.class);
		when(dao.getAll()).thenReturn(List.of(tokens));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = Constants.MAPPER.getFactory().createGenerator(out))
		{
			new TokensIE(dao, Constants.MAPPER).serialize(generator);
		}
		return Constants.MAPPER.readTree(out.toString());
	}

	private static JsonNode exportedContents(JsonNode exportedToken)
	{
		return JsonUtil.parse(Base64.getDecoder().decode(exportedToken.get("contents").asText()));
	}

	private static Token token(String type, String contents)
	{
		Token token = new Token(type, "v-" + type, 1L);
		token.setContents(contents.getBytes(StandardCharsets.UTF_8));
		return token;
	}
}
