/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
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
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * An integration test of token refresh flow
 * @author P.Piernik
 *
 */
public class RefreshTokenTest extends TokenTestBase
{
	private RefreshToken initRefresh(List<String> scope, ClientAuthentication ca) throws Exception
	{
		return init(scope, ca).getTokens().getRefreshToken();	
	}
	

	@Test
	public void shouldRefreshToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		AccessTokenResponse parsedResp = getRefreshedAccessToken(refreshToken, ca, "foo", "bar");
		BearerAccessToken bearerToken = (BearerAccessToken) parsedResp.getTokens().getAccessToken();
		
		assertThat(bearerToken.getLifetime(), is(3600l));
		assertThat(bearerToken.getScope(), is(new Scope("foo", "bar")));
		assertThat(bearerToken.getType(), is(AccessTokenType.BEARER));
	}

	@Test
	public void shouldAssumeOriginalScopesWhenNoScopesAreRequestedUponRefresh() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		AccessTokenResponse parsedResp = getRefreshedAccessToken(refreshToken, ca);
		BearerAccessToken bearerToken = (BearerAccessToken) parsedResp.getTokens().getAccessToken();
		
		assertThat(bearerToken.getLifetime(), is(3600l));
		assertThat(bearerToken.getScope(), is(new Scope("foo", "bar")));
		assertThat(bearerToken.getType(), is(AccessTokenType.BEARER));
	}
	
	@Test
	public void refreshedTokenCanBeUsedToObtainUserInfo() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		JWTClaimsSet claimSet = refreshAndGetUserInfo(refreshToken, ca, "foo", "bar");

		assertThat(claimSet.getClaim("c"), is("PL"));
		assertThat(claimSet.getClaim("email"), is("example@example.com"));
		assertThat(claimSet.getClaim("sub"), is("userA"));
	}

	@Test
	public void shouldRefreshTokenWithIdToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("openid"), ca);

		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new RefreshTokenGrant(refreshToken), new Scope("openid"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse refreshResp = wrapped.send();
		AccessTokenResponse refreshParsedResp = AccessTokenResponse.parse(refreshResp);
		assertThat(refreshParsedResp.getTokens().getAccessToken(), notNullValue());
		assertThat(refreshParsedResp.getCustomParameters().get("id_token"), notNullValue());
	
	}

	@Test
	public void shouldDenyToRefreshTokenWithIncorrectScope() throws Exception
	{

		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		// check wrong scope
		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new RefreshTokenGrant(refreshToken), new Scope("xx"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode(), is(HTTPResponse.SC_BAD_REQUEST));
	}

	@Test
	public void shouldDenyToRefreshTokenByAnotherClient() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));
		ClientAuthentication ca2 = new ClientSecretBasic(new ClientID("client2"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca2,
				new RefreshTokenGrant(refreshToken), new Scope("foo"));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);

		HTTPResponse errorResp = wrapped.send();
		assertThat(errorResp.getStatusCode(), is(HTTPResponse.SC_BAD_REQUEST));

	}
	
	@Test
	public void shouldRefreshUserInfoAfterRefreshToken() throws Exception
	{
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"),
				new Secret("clientPass"));

		RefreshToken refreshToken = initRefresh(Arrays.asList("foo", "bar"), ca);

		JWTClaimsSet claimSet = refreshAndGetUserInfo(refreshToken, ca, "foo", "bar");	
		
		assertThat(claimSet.getClaim("c"), is("PL"));
		
		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		attrsMan.setAttribute(new EntityParam(identity),
				StringAttribute.of("c", "/oauth-users", "new"));
		
		claimSet = refreshAndGetUserInfo(refreshToken, ca, "foo", "bar");
		assertThat(claimSet.getClaim("c"), is("new"));	
		
	}

	private AccessTokenResponse getRefreshedAccessToken(RefreshToken token, 
			ClientAuthentication ca, String... scopes) throws Exception
	{
		TokenRequest refreshRequest = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new RefreshTokenGrant(token), new Scope(scopes));

		HTTPRequest bare = refreshRequest.toHTTPRequest();
		CustomHTTPSRequest wrapped = new CustomHTTPSRequest(bare,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		HTTPResponse refreshResp = wrapped.send();
		return AccessTokenResponse.parse(refreshResp);
	}
	
	private JWTClaimsSet refreshAndGetUserInfo(RefreshToken token, 
			ClientAuthentication ca, String... scopes) throws Exception
	{
		AccessTokenResponse refreshParsedResp = getRefreshedAccessToken(token, ca, scopes);
		assertThat(refreshParsedResp.getTokens().getAccessToken(), notNullValue());
		
		return getUserInfo(refreshParsedResp.getTokens().getAccessToken());
	}

	private JWTClaimsSet getUserInfo(AccessToken accessToken) throws Exception
	{
		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI("https://localhost:52443/oauth/userinfo"),
				(BearerAccessToken) accessToken);
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new CustomHTTPSRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse uiHttpResponse = wrapped2.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		UserInfo ui = uiResponseS.getUserInfo();
		JWTClaimsSet claimSet = ui.toJWTClaimsSet();
		return claimSet;
	}
}
