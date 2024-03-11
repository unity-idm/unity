/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.token.access.TokenTestBase;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

/**
 * An integration test of the Token endpoint. The context is initialized
 * internally (i.e. the state which should be present after the client's &
 * user's interaction with the web authZ endpoint. Then the authz code is
 * exchanged for the access token and the user profile is fetched.
 * 
 * @author K. Benedyczak
 */
public class TokenEndpointTest extends TokenTestBase
{
	@BeforeEach
	public void init()
	{
		super.setupPlain(RefreshTokenIssuePolicy.ALWAYS);
	}
	
	@Test
	public void shouldReturnCompleteBearerTokenAfterCodeFlow() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensMan), ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		BearerAccessToken bearerToken = (BearerAccessToken) parsedResp.getTokens().getAccessToken();
		
		assertThat((int)bearerToken.getLifetime()).isEqualTo(OAuthTestUtils.DEFAULT_ACCESS_TOKEN_VALIDITY);
		assertThat(bearerToken.getScope()).isEqualTo(new Scope("sc1"));
		assertThat(bearerToken.getType()).isEqualTo(AccessTokenType.BEARER);
	}
	
	@Test
	public void shouldFailGetTokenWithoutWrongCredential() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensMan), ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("wrong"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode()).isEqualTo(HTTPResponse.SC_FORBIDDEN);
	}
	
	@Test
	public void shouldFailGetTokenWithoutCredential() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensMan), ctx);

		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), new ClientID("client1"),
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode()).isEqualTo(HTTPResponse.SC_UNAUTHORIZED);
	}
	
	
	@Test
	public void shouldReturnUserInfoAfterCompleteCodeFlow() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensMan), ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);

		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI("https://localhost:52443/oauth/userinfo"),
				(BearerAccessToken) parsedResp.getTokens().getAccessToken());
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new HttpRequestConfigurer().secureRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse uiHttpResponse = wrapped2.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		UserInfo ui = uiResponseS.getUserInfo();
		JWTClaimsSet claimSet = ui.toJWTClaimsSet();

		assertEquals("PL", claimSet.getClaim("c"));
		assertEquals("example@example.com", claimSet.getClaim("email"));
		assertEquals("userA", claimSet.getClaim("sub"));
	}

	@Test
	public void nonceIsReturnedInClaimSetForOIDCRequest() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(OAuthTestUtils.getOIDCConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId(), "nonce-VAL");
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensMan), ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		OIDCTokenResponse parsedResp = OIDCTokenResponse.parse(resp2);
		JWTClaimsSet claimSet = parsedResp.getOIDCTokens().getIDToken().getJWTClaimsSet();
		assertThat(claimSet.getClaim("nonce")).isEqualTo("nonce-VAL");
	}

	@Test
	public void shouldReturnAccessTokenAfterClientCredentialsFlow() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new ClientCredentialsGrant(), new Scope("foo"));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		BearerAccessToken bearerToken = (BearerAccessToken) parsedResp.getTokens().getAccessToken();
		
		assertThat(bearerToken.getLifetime()).isEqualTo(3600l);
		assertThat(bearerToken.getScope()).isEqualTo(new Scope("foo"));
		assertThat(bearerToken.getType()).isEqualTo(AccessTokenType.BEARER);
	}

	
	@Test
	public void shouldReturnUserInfoAfterCompleteClientCredentialsFlow() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new ClientCredentialsGrant(), new Scope("foo"));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);

		JSONObject parsed = getTokenInfo(parsedResp.getTokens().getAccessToken());
		System.out.println(parsed);
		assertEquals("client1", parsed.get("sub"));
		assertEquals("client1", parsed.get("client_id"));
		assertEquals("foo", ((JSONArray) parsed.get("scope")).get(0));
		assertNotNull(parsed.get("exp"));
	}
	
	@Test
	public void effectiveScopesAreReturnedWhenDifferentFromRequestedInClientCredentialsFlow() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new ClientCredentialsGrant(), new Scope("foo", "missing"));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		AccessToken accessToken = parsedResp.getTokens().getAccessToken();
		assertThat(accessToken.getScope()).isNotNull();
		assertThat(accessToken.getScope().contains("foo")).isEqualTo(true);
		assertThat(accessToken.getScope().size()).isEqualTo(1);
	}
}
