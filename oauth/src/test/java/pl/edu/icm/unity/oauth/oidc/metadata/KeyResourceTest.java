/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.oidc.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

public class KeyResourceTest
{
	@Test
	public void shouldGetJWKSet() throws IOException, ParseException
	{
		HttpRequestConfigurer reqFactory = mock(HttpRequestConfigurer.class);
		HTTPRequest wrapped = mock(HTTPRequest.class);
		HTTPResponse response = mock(HTTPResponse.class);
		when(reqFactory.secureRequest(any(), any(), any())).thenReturn(wrapped);
		when(wrapped.send()).thenReturn(response);
		when(response.getCacheControl()).thenReturn(null);
		when(response.getBody()).thenReturn("{\n" + "\"keys\": [\n" + "{\n"
				+ "\"n\": \"tLZpmdBD-qb8fwqg-DKX8ljpCAAv5n9s5N-JBzOIu3Ry1au3diX_AXKcnpqWJt3Mh3lT4x-zKl4SLpcjpSHYdim4tmqKucUupLTXS-yIqGBw2xDaI0GpYd8QFiFAxTAcwrEoCdl3BGGojo4zmARcHBe_IfeQls097Um3Xu2uiD0RehagoXnDhzk54WAvN05GXJ1xzzx6B7H_fclXcUYb5p5n7SgPDUchTDsDFGCI60Sqqz10d_GNcceThotlXRXcGVlTQ9AGJ_ejzkLWE7NiJc7ZWkrufsNKvVsWT12y66u0VWeopuQZxqSoHIRvSZ71JsBT3dAN897ViZtyYdWoqQ\",\n"
				+ "\"kid\": \"8e0acf891e090091ef1a5e7e64bab280fd1447fa\",\n" + "\"alg\": \"RS256\",\n"
				+ "\"use\": \"sig\",\n" + "\"kty\": \"RSA\",\n" + "\"e\": \"AQAB\"\n" + "},\n" + "{\n"
				+ "\"n\": \"pHEcyF7IosBA_2jKZ0iZt7oLSKcUZFoDdDsyx27xE3tIpYDMpOZATrMePFQKdow0rkhoydTq0YK9RSsW7bh1ORDXb2s4Z6HOVJiDVqtfIfH5ohKSBedaGihYN8RnZIO_XkrlDPztIZxvmsDC5mZnk0wKID4S2gstZlYqx9cblAA9o1rzr_7pf-bs1b9kyX15DNFY_8LJsDBGzRozAukkIIEgfdXG3dBvhyDESh-8qfPeL_I2w8GdY4bHtUTcUmpk1G_UzC74Zv8YGfQVY9ptjw2MTFgnctc0HBjCshioFpt6vSdi-SpyCEr7xx-JLy4YgcsmXBvcBpgh31LqZcylTw\",\n"
				+ "\"use\": \"sig\",\n" + "\"kty\": \"RSA\",\n"
				+ "\"kid\": \"a29abc19be27fb4151aa431e94fa3680ae458da5\",\n" + "\"e\": \"AQAB\",\n"
				+ "\"alg\": \"RS256\"\n" + "}\n" + "]\n" + "}");
		JWKSetRequest request = JWKSetRequest.builder()
				.withHostnameChecking(ServerHostnameCheckingMode.NONE)
				.withUrl("https://mock.google")
				.build();
		KeyResource keyResource = new KeyResource(reqFactory);
		JWKSet jwkSet = keyResource.getJWKSet(request);

		assertEquals(false, jwkSet.getKeys()
				.isEmpty());
	}
}
