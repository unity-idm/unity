/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration.to3_12;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.impl.tokens.TokenRDBMSStore;

public class TestInDBUpdateFromSchema3_9
{
	private static final String MULTI_AUDIENCE = "{\"userInfo\":\"{\\\"name\\\":\\\"Default Administrator\\\",\\\"sub\\\":\\\"12a1197c-9c89-44b9-b889-740263cdb6b7\\\","
			+ "\\\"picture\\\":\\\"url\\\",\\\"username\\\":\\\"a\\\"}\","
			+ "\"openidInfo\":\"eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0OjI0NDNcL29hdXRoMiIsInN1YiI6IjEyYTExOTdjLTljODktNDRiOS1iODg5LTc0MDI2M2NkYjZiNyIsImF1ZCI6Im9hdXRoLWNsaWVudCIsImV4cCI6MTY2NTkwODMwOCwiaWF0IjoxNjY1OTA0NzA4fQ.NK3QnylysQgFKY9801dnt_BSMZ6N53XZqlEnPp4iXiRCS-8v9o_qdRnQFuBkqtVotBfi9ZbRVZQk8796IUy_WZg9euQVvGgIKQCYKSp54JsMvP7cDyJ0cpoeMuclLrZ62L-PDJm1PKGXCCoJkrnP2qErVne4xDyjSwFwmEwSmsQVGBjEA8TXl6hcAha4cl5hpL5SdxTagGgmd_Hs0zfpyTjwqR2XByOb9fV6-NOHluatcnKp-EROPn_o-Vdi0uyY7ENbP2Q-ftiqG0KHoGN2HDS6IRypBrAqBhtOnQAIZ35PnFrsKlMVZu4nN-9B6cJq_upaWlliLke3_JicPLivRKimGLEQyjvQb3cKy1Jj3YKKO1DK-rr5UOJXfm_Yffw7l8qlOMXmn2re0274R3OsTTOxS9CD4piGlG5crucQ5xK7I2Z5ye6WUcRSM76jmn-6_HeY3xudeulYdnGPUDojJB43F0NhQcB49ihPur_vYLsS4-GvrZ4o0WWbgQ7jtUUb3Y5lDgJTj7a7FMGtQHAmGmvGpqtAUtQLY5bcjC84zcLAZsDIYLsDZA3HyD0qvMy0QOmZy-G6ikHZ5os2jdqDLETLjteFT0ODuR2wO6olwaWYCmcLkSjeFkGR2eJP4MH3ljgYtgUt0Q5VQN34dkv2sKFYD5BxOr3M5mtD5jxgGAk\","
			+ "\"authzCode\":\"-xWGHBib3P6oGQVxx1neQMxHwiJnaX7vhdUV7GiBEXQ\",\"accessToken\":\"tY0x-AKE71CWesO9S28BoPgMTF1nFijHoFzm10YYL7U\","
			+ "\"refreshToken\":null,\"firstRefreshRollingToken\":null,\"effectiveScope\":[\"openid\",\"profile\"],"
			+ "\"requestedScope\":[\"openid\",\"profile\"],"
			+ "\"redirectUri\":\"https://localhost:2443/unitygw/oauth2ResponseConsumer\","
			+ "\"subject\":\"12a1197c-9c89-44b9-b889-740263cdb6b7\",\"clientName\":null,"
			+ "\"clientUsername\":\"oauth-client\",\"maxExtendedValidity\":0,\"tokenValidity\":3600,"
			+ "\"responseType\":\"code\",\"audience\":[\"oauth-client\"],"
			+ "\"issuerUri\":\"https://localhost:2443/oauth2\",\"clientType\":\"CONFIDENTIAL\","
			+ "\"pkcsInfo\":{\"codeChallenge\":null,\"codeChallengeMethod\":null},\"clientId\":3}";

	private static final String SINGLE_AUDIENCE = "{\"userInfo\":\"{\\\"name\\\":\\\"Default Administrator\\\",\\\"sub\\\":\\\"12a1197c-9c89-44b9-b889-740263cdb6b7\\\","
			+ "\\\"picture\\\":\\\"url\\\",\\\"username\\\":\\\"a\\\"}\","
			+ "\"openidInfo\":\"eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0OjI0NDNcL29hdXRoMiIsInN1YiI6IjEyYTExOTdjLTljODktNDRiOS1iODg5LTc0MDI2M2NkYjZiNyIsImF1ZCI6Im9hdXRoLWNsaWVudCIsImV4cCI6MTY2NTkwODMwOCwiaWF0IjoxNjY1OTA0NzA4fQ.NK3QnylysQgFKY9801dnt_BSMZ6N53XZqlEnPp4iXiRCS-8v9o_qdRnQFuBkqtVotBfi9ZbRVZQk8796IUy_WZg9euQVvGgIKQCYKSp54JsMvP7cDyJ0cpoeMuclLrZ62L-PDJm1PKGXCCoJkrnP2qErVne4xDyjSwFwmEwSmsQVGBjEA8TXl6hcAha4cl5hpL5SdxTagGgmd_Hs0zfpyTjwqR2XByOb9fV6-NOHluatcnKp-EROPn_o-Vdi0uyY7ENbP2Q-ftiqG0KHoGN2HDS6IRypBrAqBhtOnQAIZ35PnFrsKlMVZu4nN-9B6cJq_upaWlliLke3_JicPLivRKimGLEQyjvQb3cKy1Jj3YKKO1DK-rr5UOJXfm_Yffw7l8qlOMXmn2re0274R3OsTTOxS9CD4piGlG5crucQ5xK7I2Z5ye6WUcRSM76jmn-6_HeY3xudeulYdnGPUDojJB43F0NhQcB49ihPur_vYLsS4-GvrZ4o0WWbgQ7jtUUb3Y5lDgJTj7a7FMGtQHAmGmvGpqtAUtQLY5bcjC84zcLAZsDIYLsDZA3HyD0qvMy0QOmZy-G6ikHZ5os2jdqDLETLjteFT0ODuR2wO6olwaWYCmcLkSjeFkGR2eJP4MH3ljgYtgUt0Q5VQN34dkv2sKFYD5BxOr3M5mtD5jxgGAk\","
			+ "\"authzCode\":\"-xWGHBib3P6oGQVxx1neQMxHwiJnaX7vhdUV7GiBEXQ\",\"accessToken\":\"tY0x-AKE71CWesO9S28BoPgMTF1nFijHoFzm10YYL7U\","
			+ "\"refreshToken\":null,\"firstRefreshRollingToken\":null,\"effectiveScope\":[\"openid\",\"profile\"],"
			+ "\"requestedScope\":[\"openid\",\"profile\"],"
			+ "\"redirectUri\":\"https://localhost:2443/unitygw/oauth2ResponseConsumer\","
			+ "\"subject\":\"12a1197c-9c89-44b9-b889-740263cdb6b7\",\"clientName\":null,"
			+ "\"clientUsername\":\"oauth-client\",\"maxExtendedValidity\":0,\"tokenValidity\":3600,"
			+ "\"responseType\":\"code\",\"audience\":\"oauth-client\","
			+ "\"issuerUri\":\"https://localhost:2443/oauth2\",\"clientType\":\"CONFIDENTIAL\","
			+ "\"pkcsInfo\":{\"codeChallenge\":null,\"codeChallengeMethod\":null},\"clientId\":3}";

	@Test
	public void shouldMigrateAudienceInOAuthToken() throws IOException
	{
		TokenRDBMSStore tokensDAO = mock(TokenRDBMSStore.class);
		Token invalid = new Token("oauth2Access", "123", 1l);

		invalid.setContents(SINGLE_AUDIENCE.getBytes(StandardCharsets.UTF_8));
		when(tokensDAO.getAll()).thenReturn(Lists.newArrayList(invalid));
		InDBUpdateFromSchema17 hotfix = new InDBUpdateFromSchema17(tokensDAO);

		hotfix.updateOAuthTokens();

		Token expectedToken = new Token("oauth2Access", "123", 1l);
		expectedToken.setContents(MULTI_AUDIENCE.getBytes(StandardCharsets.UTF_8));
		verify(tokensDAO).update(expectedToken);
	}
}
