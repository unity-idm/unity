/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

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
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;

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

	@Test
	public void testCodeFlow() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(tokensMan, ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);

		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI("https://localhost:52443/oauth/userinfo"),
				(BearerAccessToken) parsedResp.getTokens().getAccessToken());
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new CustomHTTPSRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse uiHttpResponse = wrapped2.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		UserInfo ui = uiResponseS.getUserInfo();
		JWTClaimsSet claimSet = ui.toJWTClaimsSet();

		Assert.assertEquals("PL", claimSet.getClaim("c"));
		Assert.assertEquals("example@example.com", claimSet.getClaim("email"));
		Assert.assertEquals("userA", claimSet.getClaim("sub"));
	}

	@Test
	public void nonceIsReturnedInClaimSetForOIDCRequest() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId(), "nonce-VAL");
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(tokensMan, ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();

		OIDCTokenResponse parsedResp = OIDCTokenResponse.parse(resp2);
		JWTClaimsSet claimSet = parsedResp.getOIDCTokens().getIDToken().getJWTClaimsSet();
		assertThat(claimSet.getClaim("nonce"), is("nonce-VAL"));
	}

	@Test
	public void testClientCredentialFlow() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new ClientCredentialsGrant(), new Scope("foo"));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
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
	
}
