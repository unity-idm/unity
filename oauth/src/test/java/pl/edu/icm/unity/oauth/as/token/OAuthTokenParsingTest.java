/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.oauth.as.OAuthToken;

public class OAuthTokenParsingTest
{
	@Test
	public void shouldParseTokenWithMultiAudience()
	{
		OAuthToken token = OAuthToken.getInstanceFromJson(
				("{\"userInfo\":\"{\\\"name\\\":\\\"Default Administrator\\\",\\\"sub\\\":\\\"12a1197c-9c89-44b9-b889-740263cdb6b7\\\","
						+ "\\\"picture\\\":\\\"url\\\",\\\"username\\\":\\\"a\\\"}\","
						+ "\"openidInfo\":\"eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0OjI0NDNcL29hdXRoMiIsInN1YiI6IjEyYTExOTdjLTljODktNDRiOS1iODg5LTc0MDI2M2NkYjZiNyIsImF1ZCI6Im9hdXRoLWNsaWVudCIsImV4cCI6MTY2NTkwODMwOCwiaWF0IjoxNjY1OTA0NzA4fQ.NK3QnylysQgFKY9801dnt_BSMZ6N53XZqlEnPp4iXiRCS-8v9o_qdRnQFuBkqtVotBfi9ZbRVZQk8796IUy_WZg9euQVvGgIKQCYKSp54JsMvP7cDyJ0cpoeMuclLrZ62L-PDJm1PKGXCCoJkrnP2qErVne4xDyjSwFwmEwSmsQVGBjEA8TXl6hcAha4cl5hpL5SdxTagGgmd_Hs0zfpyTjwqR2XByOb9fV6-NOHluatcnKp-EROPn_o-Vdi0uyY7ENbP2Q-ftiqG0KHoGN2HDS6IRypBrAqBhtOnQAIZ35PnFrsKlMVZu4nN-9B6cJq_upaWlliLke3_JicPLivRKimGLEQyjvQb3cKy1Jj3YKKO1DK-rr5UOJXfm_Yffw7l8qlOMXmn2re0274R3OsTTOxS9CD4piGlG5crucQ5xK7I2Z5ye6WUcRSM76jmn-6_HeY3xudeulYdnGPUDojJB43F0NhQcB49ihPur_vYLsS4-GvrZ4o0WWbgQ7jtUUb3Y5lDgJTj7a7FMGtQHAmGmvGpqtAUtQLY5bcjC84zcLAZsDIYLsDZA3HyD0qvMy0QOmZy-G6ikHZ5os2jdqDLETLjteFT0ODuR2wO6olwaWYCmcLkSjeFkGR2eJP4MH3ljgYtgUt0Q5VQN34dkv2sKFYD5BxOr3M5mtD5jxgGAk\","
						+ "\"authzCode\":\"-xWGHBib3P6oGQVxx1neQMxHwiJnaX7vhdUV7GiBEXQ\",\"accessToken\":\"tY0x-AKE71CWesO9S28BoPgMTF1nFijHoFzm10YYL7U\","
						+ "\"refreshToken\":null,\"firstRefreshRollingToken\":null,"
						+ "\"effectiveScope\":[{\"scope\":\"openid\",\"scopeDefinition\":{\"name\":\"openid\",\"description\":\"\",\"pattern\":false, \"attributes\" : []},\"pattern\":false},"
						+ "{\"scope\":\"profile\",\"scopeDefinition\":{\"name\":\"profile\",\"description\":\"\",\"pattern\":false},\"pattern\":false}],"	
						+ "\"requestedScope\":[\"openid\",\"profile\"],"
						+ "\"redirectUri\":\"https://localhost:2443/unitygw/oauth2ResponseConsumer\","
						+ "\"subject\":\"12a1197c-9c89-44b9-b889-740263cdb6b7\",\"clientName\":null,"
						+ "\"clientUsername\":\"oauth-client\",\"maxExtendedValidity\":0,\"tokenValidity\":3600,"
						+ "\"responseType\":\"code\",\"audience\":[\"oauth-client\", \"oauth-client2\"],"
						+ "\"issuerUri\":\"https://localhost:2443/oauth2\",\"clientType\":\"CONFIDENTIAL\","
						+ "\"pkcsInfo\":{\"codeChallenge\":null,\"codeChallengeMethod\":null},\"clientId\":3}")
								.getBytes());

		assertThat(token.getAudience().get(0)).isEqualTo("oauth-client");
		assertThat(token.getAudience().get(1)).isEqualTo("oauth-client2");

	}
}
