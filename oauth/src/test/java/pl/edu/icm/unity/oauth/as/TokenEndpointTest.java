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
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpointFactory;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * An integration test of the Token endpoint. The context is initialized internally (i.e. the state which should
 * be present after the client's & user's interaction with the web authZ endpoint. Then the authz code is exchanged 
 * for the access token and the user profile is fetched.
 *  
 * @author K. Benedyczak
 */
public class TokenEndpointTest extends DBIntegrationTestBase
{
	private static final String OAUTH_ENDP_CFG = 
					"unity.oauth2.as.issuerUri=https://localhost:2443/oauth2\n"
					+ "unity.oauth2.as.signingCredential=MAIN\n"
					+ "unity.oauth2.as.clientsGroup=/oauth-clients\n"
					+ "unity.oauth2.as.usersGroup=/oauth-users\n"
					+ "#unity.oauth2.as.translationProfile=\n"
					+ "unity.oauth2.as.scopes.1.name=foo\n"
					+ "unity.oauth2.as.scopes.1.description=Provides access to foo info\n"
					+ "unity.oauth2.as.scopes.1.attributes.1=stringA\n"
					+ "unity.oauth2.as.scopes.1.attributes.2=o\n"
					+ "unity.oauth2.as.scopes.1.attributes.3=email\n"
					+ "unity.oauth2.as.scopes.2.name=bar\n"
					+ "unity.oauth2.as.scopes.2.description=Provides access to bar info\n"
					+ "unity.oauth2.as.scopes.2.attributes.1=c\n";
	
	public static final String REALM_NAME = "testr";
	
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private PKIManagement pkiMan;

	private Identity clientId;
	
	@Before
	public void setup()
	{
		try
		{
			setupMockAuthn();
			clientId = OAuthTestUtils.createOauthClient(idsMan, attrsMan, groupsMan);
			createUser();
			AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 
					10, 100, -1, 600);
			realmsMan.addRealm(realm);
			List<AuthenticationOptionDescription> authnCfg = new ArrayList<>();
			authnCfg.add(new AuthenticationOptionDescription("Apass"));
			EndpointConfiguration config = new EndpointConfiguration(new I18nString("endpointIDP"),
					"desc",	authnCfg, OAUTH_ENDP_CFG, REALM_NAME);
			endpointMan.deploy(OAuthTokenEndpointFactory.NAME, "endpointIDP", "/oauth", config);
			List<EndpointDescription> endpoints = endpointMan.getEndpoints();
			assertEquals(1, endpoints.size());

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	protected void setupMockAuthn() throws Exception
	{
		setupPasswordAuthn();
		
		authnMan.createAuthenticator("Apass", "password with rest-httpbasic", null, "", "credential1");
	}
	


	/**
	 * Only simple add user so the token may be added - 
	 * attributes etc are loaded by the WebAuths endpoint which is skipped here.
	 * @throws Exception
	 */
	protected void createUser() throws Exception
	{
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "userA"), 
				"cr-pass", EntityState.valid, false);
	}
	
	@Test
	public void testCodeFlow() throws Exception
	{
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(), 
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId.getEntityId());
		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowAccessCode(tokensMan,
				ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));
		TokenRequest request = new TokenRequest(new URI("https://localhost:52443/oauth/token"), ca, 
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(), 
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"), 
				ServerHostnameCheckingMode.NONE);
		
		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		
		UserInfoRequest uiRequest = new UserInfoRequest(new URI("https://localhost:52443/oauth/userinfo"), 
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
				GrantFlow.authorizationCode, clientId.getEntityId(), "nonce-VAL");
		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowAccessCode(tokensMan, ctx);
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));
		TokenRequest request = new TokenRequest(new URI("https://localhost:52443/oauth/token"), ca, 
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
		ClientAuthentication ca = new ClientSecretBasic(new ClientID("client1"), new Secret("clientPass"));
		TokenRequest request = new TokenRequest(new URI("https://localhost:52443/oauth/token"), ca, 
				new ClientCredentialsGrant(), new Scope("foo"));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"), 
				ServerHostnameCheckingMode.NONE);
		
		HTTPResponse resp2 = wrapped.send();

		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		
		UserInfoRequest uiRequest = new UserInfoRequest(new URI("https://localhost:52443/oauth/tokeninfo"), 
				(BearerAccessToken) parsedResp.getTokens().getAccessToken());
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new CustomHTTPSRequest(bare2, pkiMan.getValidator("MAIN"), 
				ServerHostnameCheckingMode.NONE);
		HTTPResponse httpResponse = wrapped2.send();
		
		JSONObject parsed = httpResponse.getContentAsJSONObject();
		System.out.println(parsed);
		assertEquals("client1", parsed.get("sub"));
		assertEquals("client1", parsed.get("client_id"));
		assertEquals("foo", ((JSONArray)parsed.get("scope")).get(0));
		assertNotNull(parsed.get("exp"));
	}
}
