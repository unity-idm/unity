/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * Base for token endpoint test
 * @author P.Piernik
 *
 */
public abstract class TokenTestBase extends DBIntegrationTestBase
{
	protected static final String OAUTH_ENDP_CFG = "unity.oauth2.as.issuerUri=https://localhost:2443/oauth2\n"
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
			+ "unity.oauth2.as.scopes.2.attributes.1=c\n"
			+ "unity.oauth2.as.refreshTokenValidity=3600\n";

	public static final String REALM_NAME = "testr";

	@Autowired
	protected TokensManagement tokensMan;
	@Autowired
	protected PKIManagement pkiMan;
	@Autowired
	protected AuthenticatorManagement authnMan;
	@Autowired
	private AuthenticationFlowManagement authnFlowMan;
	
	protected Identity clientId1;
	
	protected IdentityParam initUser(String username) throws Exception
	{
		IdentityParam identity = new IdentityParam(UsernameIdentity.ID, "userA");
		groupsMan.addMemberFromParent("/oauth-users", new EntityParam(identity));
		aTypeMan.addAttributeType(new AttributeType("email", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("c", StringAttributeSyntax.ID));
		attrsMan.createAttribute(new EntityParam(identity),
				StringAttribute.of("email", "/oauth-users", "example@example.com"));
		attrsMan.createAttribute(new EntityParam(identity),
				StringAttribute.of("c", "/oauth-users", "PL"));
		return identity;
	}
	
	protected JSONObject getTokenInfo(AccessToken token) throws Exception
	{
		UserInfoRequest uiRequest = new UserInfoRequest(
				new URI("https://localhost:52443/oauth/tokeninfo"),
				(BearerAccessToken) token);
		HTTPRequest bare2 = uiRequest.toHTTPRequest();
		HTTPRequest wrapped2 = new CustomHTTPSRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse httpResponse = wrapped2.send();

		JSONObject parsed = httpResponse.getContentAsJSONObject();
		return parsed;
	}
	
	/**
	 * Only simple add user so the token may be added - attributes etc are
	 * loaded by the WebAuths endpoint which is skipped here.
	 * 
	 * @throws Exception
	 */
	protected void createUser() throws Exception
	{
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "userA"), "cr-pass",
				EntityState.valid, false);
	}
	
	protected void setupMockAuthn() throws Exception
	{
		setupPasswordAuthn();

		authnMan.createAuthenticator("Apass", "password", "", "credential1");
	}
	
	@Before
	public void setup()
	{
		try
		{
			setupMockAuthn();
			clientId1 = OAuthTestUtils.createOauthClient(idsMan, attrsMan, groupsMan,
					eCredMan, "client1");
			OAuthTestUtils.createOauthClient(idsMan, attrsMan, groupsMan,
					eCredMan, "client2");

			createUser();
			AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 10, 100,
					RememberMePolicy.disallow , 1, 600);
			realmsMan.addRealm(realm);
			authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
					"flow1", Policy.NEVER,
					Sets.newHashSet("Apass")));
			
			EndpointConfiguration config = new EndpointConfiguration(
					new I18nString("endpointIDP"), "desc", Lists.newArrayList("flow1"),
					OAUTH_ENDP_CFG, REALM_NAME);
			endpointMan.deploy(OAuthTokenEndpoint.NAME, "endpointIDP", "/oauth",
					config);
			List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
			assertThat(endpoints.size(), is(1));

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Initialize environment before token refresh or exchange:
	 * -add attributes to the user
	 * -create OAuth context
	 * -prepare access token request (with refresh token) and invoke them. 
	 * @param scope requested scope in code flow
	 * @param ca user auth 
	 * @return Parsed access token response
	 * @throws Exception
	 */
	protected AccessTokenResponse init(List<String> scopes, ClientAuthentication ca)
			throws Exception
	{
		IdentityParam identity = initUser("userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());

		ctx.setRequestedScopes(new HashSet<>(scopes));
		for (String scope: scopes)
			ctx.addEffectiveScopeInfo(new ScopeInfo(scope, scope, Lists.newArrayList(scope + " attr")));
		ctx.setOpenIdMode(true);
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(tokensMan, ctx, identity);

		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new CustomHTTPSRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();
		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		assertThat(parsedResp.getTokens().getRefreshToken(), notNullValue());
		return parsedResp;
	}
}
