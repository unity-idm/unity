/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.impl.tokens.TokenRDBMSStore;

public class TestInDBUpdateFromSchema2_8
{
	private static final String INVALID = "{\"userInfo\":\"{\\\"sub\\\":\\\"userA\\\",\\\"email\\\":\\\"example@example.com\\\"}\","
			+ "\"openidInfo\":\"eyJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiQ2lxUTNFTWhmdzhxVDZ5MnFaam41dyIsInN1YiI6InVzZXJBIiwiYXVkIjoiY2xpZW50QyIsImlzcyI6Imh0dHBzOlwvXC9sb2NhbGhvc3Q6MjMzXC9mb29cL3Rva2VuIiwiZXhwIjoxNTc2MzYxNDczLCJpYXQiOjE1NzYzNjExNzMsIm5vbmNlIjoibm9uY2UifQ.A0zSrhWEuPd6QYaB56RupejBPEYmnk19LNI5klt92GMMXm9wCh788BTA6UkU_ZH1fsV17LAr6hEtp1ttKSaK2WLugX80doO3t1GNKlGvXPo8-2kuZiLKD3WGc3IADh8fdAIyncoMWlv2UQGXhIneo6TqAYmqOo08NOziJk524X7IgW2a3HPEq24s_pDgbLPI7t6LbLutrc87f5AmemXiRJgfAEst6jtYFq7actDUnqKfUzAExPeXfWzdcAqB2d3Nv45bdwiSgAPBnVwe9iuXE1A7zlb6QnfnmiYJ3-x7f0HvgEOkLzJ5SAxl7_bvRnzStIczzFWfETpkZGbzcqV8Yg\","
			+ "\"authzCode\":null,\"accessToken\":\"_b2dWe2BLLfDj-NgohNCnfcrH1W1h6c782nmYcvXiaQ\","
			+ "\"refreshToken\":null,\"effectiveScope\":[\"sc1\"],\"requestedScope\":[],"
			+ "\"redirectUri\":\"https://return.host.com/foo\",\"subject\":\"userA\","
			+ "\"clientName\":null,\"clientUsername\":\"clientC\",\"maxExtendedValidity\":0,"
			+ "\"tokenValidity\":100,\"responseType\":\"id_token token\",\"audience\":\"clientC\","
			+ "\"issuerUri\":\"https://localhost:233/foo/token\",\"clientType\":\"CONFIDENTIAL\","
			+ "\"codeChallenge\":null,\"codeChallengeMethod\":null,"
			+ "\"clientId\":100}";

	private static final String VALID = "{\"userInfo\":\"{\\\"sub\\\":\\\"userA\\\",\\\"email\\\":\\\"example@example.com\\\"}\","
			+ "\"openidInfo\":\"eyJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiQ2lxUTNFTWhmdzhxVDZ5MnFaam41dyIsInN1YiI6InVzZXJBIiwiYXVkIjoiY2xpZW50QyIsImlzcyI6Imh0dHBzOlwvXC9sb2NhbGhvc3Q6MjMzXC9mb29cL3Rva2VuIiwiZXhwIjoxNTc2MzYxNDczLCJpYXQiOjE1NzYzNjExNzMsIm5vbmNlIjoibm9uY2UifQ.A0zSrhWEuPd6QYaB56RupejBPEYmnk19LNI5klt92GMMXm9wCh788BTA6UkU_ZH1fsV17LAr6hEtp1ttKSaK2WLugX80doO3t1GNKlGvXPo8-2kuZiLKD3WGc3IADh8fdAIyncoMWlv2UQGXhIneo6TqAYmqOo08NOziJk524X7IgW2a3HPEq24s_pDgbLPI7t6LbLutrc87f5AmemXiRJgfAEst6jtYFq7actDUnqKfUzAExPeXfWzdcAqB2d3Nv45bdwiSgAPBnVwe9iuXE1A7zlb6QnfnmiYJ3-x7f0HvgEOkLzJ5SAxl7_bvRnzStIczzFWfETpkZGbzcqV8Yg\","
			+ "\"authzCode\":null,\"accessToken\":\"_b2dWe2BLLfDj-NgohNCnfcrH1W1h6c782nmYcvXiaQ\","
			+ "\"refreshToken\":null,\"effectiveScope\":[\"sc1\"],\"requestedScope\":[],"
			+ "\"redirectUri\":\"https://return.host.com/foo\",\"subject\":\"userA\","
			+ "\"clientName\":null,\"clientUsername\":\"clientC\",\"maxExtendedValidity\":0,"
			+ "\"tokenValidity\":100,\"responseType\":\"id_token token\",\"audience\":\"clientC\","
			+ "\"issuerUri\":\"https://localhost:233/foo/token\",\"clientType\":\"CONFIDENTIAL\","
			+ "\"clientId\":100,"
			+ "\"pkcsInfo\":{\"codeChallenge\":null,\"codeChallengeMethod\":null}}";

	@Test
	public void shouldFixInvalidJson() throws IOException
	{
		TokenRDBMSStore tokensDAO = mock(TokenRDBMSStore.class);
		Token invalid = new Token("oauth2Access", "123", 1l);
		
		invalid.setContents(INVALID.getBytes(StandardCharsets.UTF_8));
		when(tokensDAO.getAll()).thenReturn(Lists.newArrayList(invalid));
		InDBUpdateFromSchema8 hotfix = new InDBUpdateFromSchema8(tokensDAO);
		
		hotfix.updateTokens();
		
		Token expectedToken = new Token("oauth2Access", "123", 1l);
		expectedToken.setContents(VALID.getBytes(StandardCharsets.UTF_8));
		verify(tokensDAO).update(expectedToken);
	}
}
