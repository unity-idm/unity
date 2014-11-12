/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpointFactory;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Somewhat complex integration test of the OAuth RP authenticator.
 * 
 * Unity AS endpoint is deployed with password authN and JWTManagement endpoint with the tested OAuth RP
 * authenticator. 
 *  
 * First an access token is generated and recorded in internal tokens store. Then this token is used to access the 
 * JWT management endpoint. The authenticator is configured to check it against the Unity AS endpoint. 
 * @author K. Benedyczak
 */
public class OAuthRPAuthenticatorTest extends DBIntegrationTestBase
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

	private static final String OAUTH_RP_CFG = 
			"unity.oauth2-rp.profileEndpoint=https://localhost:2443/oauth2\n"
			+ "unity.oauth2-rp.cacheTime=20\n"
			+ "unity.oauth2-rp.verificationProtocol=unity\n"
			+ "unity.oauth2-rp.verificationEndpoint=\n"
			+ "unity.oauth2-rp.clientId=\n"
			+ "unity.oauth2-rp.clientSecret=\n"
			+ "unity.oauth2-rp.clientAuthenticationMode=\n"
			+ "unity.oauth2-rp.opeinidConnectMode=\n"
			+ "unity.oauth2-rp.httpClientTruststore=\n"
			+ "unity.oauth2-rp.httpClientHostnameChecking=\n"
			+ "unity.oauth2-rp.translationProfile=\n";
	public static final String REALM_NAME = "testr";
	
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

}
