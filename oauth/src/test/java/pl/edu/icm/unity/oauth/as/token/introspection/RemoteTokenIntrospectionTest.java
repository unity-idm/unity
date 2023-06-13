/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.nimbusds.common.contenttype.ContentType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionSuccessResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.MockPKIMan;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;
import pl.edu.icm.unity.rest.jwt.JWTUtils;

public class RemoteTokenIntrospectionTest
{

	@Test
	public void shouldReturnInactiveWhenNoConfigForIssuer() throws ParseException, JOSEException, EngineException
	{
		IntrospectionServiceContextProvider introspectionServiceContextProvider = mock(
				IntrospectionServiceContextProvider.class);
		RemoteTokenIntrospectionService remoteTokenIntrospectionService = new RemoteTokenIntrospectionService(
				introspectionServiceContextProvider);
		when(introspectionServiceContextProvider.getRemoteServiceContext("unknown")).thenReturn(Optional.empty());
		SignedJWT jwts = SignedJWT.parse(
				JWTUtils.generate(new MockPKIMan().getCredential("MAIN"), new JWTClaimsSet.Builder().issuer("issuer")
						.build()));
		SignedJWTWithIssuer signedJWTWithIssuer = new SignedJWTWithIssuer(jwts);
		
		
		Response r = remoteTokenIntrospectionService.processRemoteIntrospection(signedJWTWithIssuer);
		JSONObject parsed = (JSONObject) JSONValue.parse((r.getEntity()
				.toString()));
	
		
		
		assertThat(parsed.getAsString("active")).isEqualTo("false");
		assertThat(parsed.size()).isEqualTo(1);
	}

	@Test
	public void shouldCallRemoteIntrospectionService()
			throws ParseException, JOSEException, EngineException, IOException, com.nimbusds.oauth2.sdk.ParseException
	{
		IntrospectionServiceContextProvider introspectionServiceContextProvider = mock(
				IntrospectionServiceContextProvider.class);
		HttpRequestConfigurer httpRequestConfigurer = mock(HttpRequestConfigurer.class);
		HTTPRequest mockRequest = mock(HTTPRequest.class);
		JWSVerifier jwsVerifier = mock(JWSVerifier.class);
		RemoteTokenIntrospectionService remoteTokenIntrospectionService = new RemoteTokenIntrospectionService(
				introspectionServiceContextProvider, httpRequestConfigurer);
		when(introspectionServiceContextProvider.getRemoteServiceContext("issuer"))
				.thenReturn(Optional.of(RemoteIntrospectionServiceContext.builder()
						.withUrl(new URL("https://test.com"))
						.withClientId("id")
						.withClientSecret("secret")
						.withVerifier(jwsVerifier)
						.build()));
		when(httpRequestConfigurer.secureRequest(any(), any(), any())).thenReturn(mockRequest);
		when(jwsVerifier.verify(any(), any(), any())).thenReturn(true);
		HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_OK);
		TokenIntrospectionResponse resp = new TokenIntrospectionSuccessResponse(new JSONObject(Map.of("active", true)));
		httpResponse.setContent(resp.toSuccessResponse()
				.toJSONObject()
				.toJSONString());
		httpResponse.setContentType(ContentType.APPLICATION_JSON.toString());
		when(mockRequest.send()).thenReturn(httpResponse);
		SignedJWT jwts = SignedJWT.parse(JWTUtils.generate(new MockPKIMan().getCredential("MAIN"),
				new JWTClaimsSet.Builder().issuer("issuer")
						.expirationTime(Date.from(Instant.now()
								.plusSeconds(100)))
						.build()));
		SignedJWTWithIssuer signedJWTWithIssuer = new SignedJWTWithIssuer(jwts);
	
		
		Response r = remoteTokenIntrospectionService.processRemoteIntrospection(signedJWTWithIssuer);

		
		JSONObject parsed = (JSONObject) JSONValue.parse((r.getEntity()
				.toString()));
		assertThat(parsed.getAsString("active")).isEqualTo("true");
		assertThat(parsed.size()).isEqualTo(1);
	}

}
