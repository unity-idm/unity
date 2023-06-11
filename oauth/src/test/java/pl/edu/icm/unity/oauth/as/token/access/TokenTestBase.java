/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

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
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthScope;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Base for token endpoint test
 * @author P.Piernik
 *
 */
public abstract class TokenTestBase extends DBIntegrationTestBase
{
	private static final String OAUTH_ENDP_CFG = "unity.oauth2.as.issuerUri=https://localhost:2443/oauth2\n"
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
			+ "unity.oauth2.as.scopes.99.name=" + OIDCScopeValue.OFFLINE_ACCESS.getValue() + "\n"
			+ "unity.oauth2.as.refreshTokenValidity=3600\n";
			
	

	private static final String OIDC_ENDP_CFG = OAUTH_ENDP_CFG 
			+ "unity.oauth2.as.scopes.3.name=openid\n";

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
		HTTPRequest wrapped2 = new HttpRequestConfigurer().secureRequest(bare2, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse httpResponse = wrapped2.send();

		JSONObject parsed = httpResponse.getContentAsJSONObject();
		return parsed;
	}
	
	/**
	 * Only simple add user so the token may be added - attributes etc are
	 * loaded by the WebAuths endpoint which is skipped here.
	 */
	protected void createUser() throws Exception
	{
		idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "userA"), "cr-pass",
				EntityState.valid);
	}
	
	protected void setupMockAuthn() throws Exception
	{
		setupPasswordAuthn();

		authnMan.createAuthenticator("Apass", "password", "", "credential1");
	}
	
	protected void setupPlain(RefreshTokenIssuePolicy refreshTokenPolicy)
	{
		setup(false, refreshTokenPolicy);
	}

	protected void setupOIDC(RefreshTokenIssuePolicy refreshTokenPolicy)
	{
		setup(true, refreshTokenPolicy);
	}
	
	private void setup(boolean withOIDC, RefreshTokenIssuePolicy refreshTokenPolicy)
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
			
			String cfg = (withOIDC ? OIDC_ENDP_CFG : OAUTH_ENDP_CFG)
					+ "unity.oauth2.as.refreshTokenIssuePolicy=" + refreshTokenPolicy.toString() + "\n";
			
			EndpointConfiguration config = new EndpointConfiguration(
					new I18nString("endpointIDP"), "desc", Lists.newArrayList("flow1"),
					cfg, REALM_NAME);
			endpointMan.deploy(OAuthTokenEndpoint.NAME, "endpointIDP", "/oauth",
					config);
			List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
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
	 */
	protected AccessTokenResponse init(List<String> scopes, ClientAuthentication ca)
			throws Exception
	{
		IdentityParam identity = initUser("userA");
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(OAuthTestUtils.getOIDCConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, clientId1.getEntityId());

		ctx.setRequestedScopes(new HashSet<>(scopes));
		for (String scope: scopes)
			ctx.addEffectiveScopeInfo(OAuthScope.builder().withName(scope).withDescription(scope)
					.withAttributes(Lists.newArrayList(scope + " attr")).withEnabled(true).build());					
					
		ctx.setOpenIdMode(true);
		AuthorizationSuccessResponse resp1 = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensMan), ctx, identity);

		TokenRequest request = new TokenRequest(
				new URI("https://localhost:52443/oauth/token"), ca,
				new AuthorizationCodeGrant(resp1.getAuthorizationCode(),
						new URI("https://return.host.com/foo")));
		HTTPRequest bare = request.toHTTPRequest();
		HTTPRequest wrapped = new HttpRequestConfigurer().secureRequest(bare, pkiMan.getValidator("MAIN"),
				ServerHostnameCheckingMode.NONE);

		HTTPResponse resp2 = wrapped.send();
		AccessTokenResponse parsedResp = AccessTokenResponse.parse(resp2);
		return parsedResp;
	}
	
}
