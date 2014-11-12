/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpointFactory;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

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
	@Autowired
	private TranslationProfileManagement profilesMan;

	private Identity clientId;
	
	@Before
	public void setup()
	{
		try
		{
			setupMockAuthn();
			createClient();
			createUser();
			AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 
					10, 100, -1, 600);
			realmsMan.addRealm(realm);
			List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
			authnCfg.add(new AuthenticatorSet(Collections.singleton("Apass")));
			endpointMan.deploy(OAuthTokenEndpointFactory.NAME, "endpointIDP", "/oauth", "desc", 
					authnCfg, OAUTH_ENDP_CFG, REALM_NAME);
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
		CredentialDefinition credDef = new CredentialDefinition(
				PasswordVerificatorFactory.NAME, "credential1", "");
		credDef.setJsonConfiguration("{\"minLength\": 4, " +
				"\"historySize\": 5," +
				"\"minClassesNum\": 1," +
				"\"denySequences\": true," +
				"\"maxAge\": 30758400}");
		authnMan.addCredentialDefinition(credDef);

		CredentialRequirements cr = new CredentialRequirements("cr-pass", "", 
				Collections.singleton(credDef.getName()));
		authnMan.addCredentialRequirement(cr);
		
		authnMan.createAuthenticator("Apass", "password with rest-httpbasic", null, "", "credential1");
	}
	
	protected void createClient() throws Exception
	{
		clientId = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "client1"), 
				"cr-pass", EntityState.valid, false);
		EntityParam e1 = new EntityParam(clientId);
		idsMan.setEntityCredential(e1, "credential1", new PasswordToken("clientPass").toJson());

		groupsMan.addGroup(new Group("/oauth-clients"));
		groupsMan.addMemberFromParent("/oauth-clients", e1);
		
		attrsMan.setAttribute(e1, new EnumAttribute(OAuthSystemAttributesProvider.ALLOWED_FLOWS, 
				"/oauth-clients", AttributeVisibility.local, GrantFlow.authorizationCode.name()), 
				false);
		attrsMan.setAttribute(e1, new StringAttribute(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI, 
				"/oauth-clients", AttributeVisibility.local, "https://dummy-return.net"), false);

		attrsMan.setAttribute(e1, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE, 
				"/", AttributeVisibility.local, "Regular User"), false);
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
	public void testcodeFlow() throws Exception
	{
		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowAccessCode(tokensMan,
				clientId.getEntityId());
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
				(BearerAccessToken) parsedResp.getAccessToken());
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
}
