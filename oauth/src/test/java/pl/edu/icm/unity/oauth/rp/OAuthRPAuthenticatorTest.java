/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Somewhat complex integration test of the OAuth RP authenticator.
 * 
 * Unity JWTManagement endpoint (as an example) is deployed with the tested OAuth RP authenticator.
 * Unity OAuth endpoint is deployed with password authN.  
 * 
 * First an access token is generated and recorded in internal tokens store. Then this token is used to access the 
 * JWT management endpoint. The authenticator is configured to check it against the Unity AS endpoint. 
 * @author K. Benedyczak
 */
public class OAuthRPAuthenticatorTest extends DBIntegrationTestBase
{
	private static final String OAUTH_ENDP_CFG = 
			"unity.oauth2.as.issuerUri=https://localhost:52443/oauth2\n"
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

	private static final String OAUTH_RP_CFG = 
			"unity.oauth2-rp.profileEndpoint=https://localhost:52443/oauth/userinfo\n"
			+ "unity.oauth2-rp.cacheTime=20\n"
			+ "unity.oauth2-rp.verificationProtocol=unity\n"
			+ "unity.oauth2-rp.verificationEndpoint=https://localhost:52443/oauth/tokeninfo\n"
			+ "unity.oauth2-rp.clientId=\n"
			+ "unity.oauth2-rp.clientSecret=\n"
			+ "#unity.oauth2-rp.clientAuthenticationMode=\n"
			+ "unity.oauth2-rp.opeinidConnectMode=false\n"
			+ "unity.oauth2-rp.httpClientTruststore=MAIN\n"
			+ "unity.oauth2-rp.httpClientHostnameChecking=NONE\n"
			+ "unity.oauth2-rp.translationProfile=tr-oauth\n";

	private static final String OAUTH_RP_CFG_INTERNAL = 
			"#unity.oauth2-rp.profileEndpoint=https://localhost:52443/oauth/userinfo\n"
			+ "unity.oauth2-rp.cacheTime=2\n"
			+ "unity.oauth2-rp.verificationProtocol=internal\n"
			+ "#unity.oauth2-rp.verificationEndpoint=https://localhost:52443/oauth/tokeninfo\n"
			+ "unity.oauth2-rp.clientId=\n"
			+ "unity.oauth2-rp.clientSecret=\n"
			+ "#unity.oauth2-rp.clientAuthenticationMode=\n"
			+ "unity.oauth2-rp.requiredScopes.1=sc1\n"
			+ "unity.oauth2-rp.opeinidConnectMode=true\n"
			+ "unity.oauth2-rp.httpClientTruststore=MAIN\n"
			+ "unity.oauth2-rp.httpClientHostnameChecking=NONE\n"
			+ "unity.oauth2-rp.translationProfile=tr-oauth\n";

	private static final String OAUTH_RP_CFG_MITRE = 
			"unity.oauth2-rp.profileEndpoint=https://mitreid.org/userinfo\n"
			+ "unity.oauth2-rp.cacheTime=20\n"
			+ "unity.oauth2-rp.verificationProtocol=mitre\n"
			+ "unity.oauth2-rp.verificationEndpoint=https://mitreid.org/introspect\n"
			+ "unity.oauth2-rp.clientId=unity\n"
			+ "unity.oauth2-rp.clientSecret=unity-pass\n"
			+ "#unity.oauth2-rp.clientAuthenticationMode=\n"
			+ "unity.oauth2-rp.opeinidConnectMode=false\n"
			+ "#unity.oauth2-rp.httpClientTruststore=MAIN\n"
			+ "unity.oauth2-rp.httpClientHostnameChecking=NONE\n"
			+ "unity.oauth2-rp.translationProfile=tr-oauth\n";

	
	private static final String JWT_ENDP_CFG = 
			"unity.jwtauthn.credential=MAIN\n";

	
	public static final String REALM_NAME = "testr";
	
	
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private TranslationProfileManagement profilesMan;
	@Autowired
	private AuthenticatorManagement authnMan;	
	@Autowired
	private AuthenticationFlowManagement authnFlowMan;
	
	@Before
	public void setup()
	{
		try
		{
			setupPasswordAuthn();
			
			authnMan.createAuthenticator("a-rp", "oauth-rp", OAUTH_RP_CFG, null);
			authnMan.createAuthenticator("a-rp-int", "oauth-rp", OAUTH_RP_CFG_INTERNAL, null);
			authnMan.createAuthenticator("a-rp-mitre", "oauth-rp", OAUTH_RP_CFG_MITRE, null);
			authnMan.createAuthenticator("Apass", "password", null, "credential1");
			
			idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "userA"), 
					"cr-pass", EntityState.valid, false);
			profilesMan.addProfile(new TranslationProfile(
					JsonUtil.parse(FileUtils.readFileToString(
							new File("src/test/resources/tr-local.json")))));
			
			AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 
					10, 100, RememberMePolicy.disallow , 1, 600);
			realmsMan.addRealm(realm);
			
			authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
					"flow1", Policy.NEVER,
					Sets.newHashSet("Apass")));
			
			endpointMan.deploy(OAuthTokenEndpoint.NAME, "endpointIDP", "/oauth",
					new EndpointConfiguration(new I18nString("endpointIDP"),
							"desc", Lists.newArrayList("flow1"),
							OAUTH_ENDP_CFG, REALM_NAME));

			authnFlowMan.addAuthenticationFlow(
					new AuthenticationFlowDefinition("flow2", Policy.NEVER,
							Sets.newHashSet("a-rp")));

			endpointMan.deploy(JWTManagementEndpoint.NAME, "endpointJWT", "/jwt",
					new EndpointConfiguration(new I18nString("endpointJWT"),
							"desc", Lists.newArrayList("flow2"),
							JWT_ENDP_CFG, REALM_NAME));

			authnFlowMan.addAuthenticationFlow(
					new AuthenticationFlowDefinition("flow3", Policy.NEVER,
							Sets.newHashSet("a-rp-int")));

			endpointMan.deploy(JWTManagementEndpoint.NAME, "endpointJWT-int",
					"/jwt-int",
					new EndpointConfiguration(new I18nString("endpointJWT-int"),
							"desc", Lists.newArrayList("flow3"),
							JWT_ENDP_CFG, REALM_NAME));

			authnFlowMan.addAuthenticationFlow(
					new AuthenticationFlowDefinition("flow4", Policy.NEVER,
							Sets.newHashSet("a-rp-mitre")));
			endpointMan.deploy(JWTManagementEndpoint.NAME, "endpointJWT-mitre",
					"/jwt-mitre",
					new EndpointConfiguration(
							new I18nString("endpointJWT-mitre"), "desc",
							Lists.newArrayList("flow4"), JWT_ENDP_CFG,
							REALM_NAME));

			List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
			assertEquals(4, endpoints.size());

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	

	private void performAuthentication(String endpoint) throws Exception
	{
		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(), 
				tokensMan);
		AccessToken ac = resp1.getAccessToken();
		
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL(endpoint));
		httpReqRaw.setAuthorization(ac.toAuthorizationHeader());
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw, new BinaryCertChainValidator(true), 
				ServerHostnameCheckingMode.NONE);
		HTTPResponse response = httpReq.send();
		Assert.assertEquals(200, response.getStatusCode());
	}

	@Ignore
	@Test
	public void OauthRPAuthnWorksWithMitre() throws Exception
	{
		AccessToken ac = new BearerAccessToken("----FILL-ME-----");
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, 
				new URL("https://localhost:52443/jwt-mitre/token"));
		httpReqRaw.setAuthorization(ac.toAuthorizationHeader());
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw, new BinaryCertChainValidator(true), 
				ServerHostnameCheckingMode.NONE);
		HTTPResponse response = httpReq.send();
		Assert.assertEquals(200, response.getStatusCode());
	}
	
	@Test
	public void OauthRPAuthnWorksWithUnity() throws Exception
	{
		performAuthentication("https://localhost:52443/jwt/token");
	}
	
	@Test
	public void OauthRPAuthnWorksWithInternal() throws Exception
	{
		performAuthentication("https://localhost:52443/jwt-int/token");
	}

	/**
	 * Authentication is performed normally. Then the token is removed and the authN is repeated 
	 * - cached results should be used. Tests wait for cache expiration and repeats authN once more - should fail.
	 * 
	 * @throws Exception
	 */
	@Test
	public void OauthRPAuthnWithCache() throws Exception
	{
		//normal
		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(), 
				tokensMan);
		AccessToken ac = resp1.getAccessToken();
		
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL("https://localhost:52443/jwt-int/token"));
		httpReqRaw.setAuthorization(ac.toAuthorizationHeader());
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw, new BinaryCertChainValidator(true), 
				ServerHostnameCheckingMode.NONE);
		HTTPResponse response = httpReq.send();
		Assert.assertEquals(200, response.getStatusCode());
		
		//remove
		tokensMan.removeToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, ac.getValue());
		
		//test cached
		HTTPResponse response2 = httpReq.send();
		Assert.assertEquals(200, response2.getStatusCode());

		//wait and re-test
		Thread.sleep(2001);
		
		HTTPResponse response3 = httpReq.send();
		Assert.assertNotEquals(200, response3.getStatusCode());
	}
}
