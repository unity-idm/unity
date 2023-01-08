
package pl.edu.icm.unity.oauth.rp.local;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public class LocalOAuthRPAuthenticatorTest extends DBIntegrationTestBase
{
	private static final String OAUTH_ENDP_CFG = "unity.oauth2.as.issuerUri=https://localhost:52443/oauth2\n"
			+ "unity.oauth2.as.signingCredential=MAIN\n" + "unity.oauth2.as.clientsGroup=/oauth-clients\n"
			+ "unity.oauth2.as.usersGroup=/oauth-users\n" + "#unity.oauth2.as.translationProfile=\n"
			+ "unity.oauth2.as.scopes.1.name=foo\n"
			+ "unity.oauth2.as.scopes.1.description=Provides access to foo info\n"
			+ "unity.oauth2.as.scopes.1.attributes.1=stringA\n" + "unity.oauth2.as.scopes.1.attributes.2=o\n"
			+ "unity.oauth2.as.scopes.1.attributes.3=email\n" + "unity.oauth2.as.scopes.2.name=bar\n"
			+ "unity.oauth2.as.scopes.2.description=Provides access to bar info\n"
			+ "unity.oauth2.as.scopes.2.attributes.1=c\n" + "unity.oauth2.as.refreshTokenIssuePolicy=ALWAYS\n";

	private static final String OAUTH_RP_CFG_INTERNAL = "unity.oauth2-local-rp.requiredScopes.1=sc1\n"
			+ "unity.oauth2-local-rp.credential=credential1\n";

	private static final String JWT_ENDP_CFG = "unity.jwtauthn.credential=MAIN\n";

	public static final String REALM_NAME = "testr";

	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	private AuthenticatorManagement authnMan;
	@Autowired
	private AuthenticationFlowManagement authnFlowMan;

	private Identity client;

	@Before
	public void setup()
	{
		try
		{
			setupPasswordAuthn();

			authnMan.createAuthenticator("a-rp-int", "local-oauth-rp", OAUTH_RP_CFG_INTERNAL, null);
			authnMan.createAuthenticator("Apass", "password", null, "credential1");

			client = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "clientA"), "cr-pass", EntityState.valid);
			eCredMan.setEntityCredential(new EntityParam(client), "credential1",
					new PasswordToken("password").toJson());

			createUsernameUser("userA", InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE, DEF_PASSWORD,
					CRED_REQ_PASS);

			AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 10, 100, RememberMePolicy.disallow, 1,
					600);
			realmsMan.addRealm(realm);

			authnFlowMan.addAuthenticationFlow(
					new AuthenticationFlowDefinition("flow1", Policy.NEVER, Sets.newHashSet("Apass")));

			endpointMan.deploy(OAuthTokenEndpoint.NAME, "endpointIDP", "/oauth", new EndpointConfiguration(
					new I18nString("endpointIDP"), "desc", Lists.newArrayList("flow1"), OAUTH_ENDP_CFG, REALM_NAME));

			authnFlowMan.addAuthenticationFlow(
					new AuthenticationFlowDefinition("flow3", Policy.NEVER, Sets.newHashSet("a-rp-int")));

			endpointMan.deploy(JWTManagementEndpoint.NAME, "endpointJWT-int", "/jwt-int", new EndpointConfiguration(
					new I18nString("endpointJWT-int"), "desc", Lists.newArrayList("flow3"), JWT_ENDP_CFG, REALM_NAME));

			List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
			assertEquals(2, endpoints.size());

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void shouldAuthenticateViaLocalOAuthRP() throws Exception
	{
		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(),
				OAuthTestUtils.getOAuthProcessor(tokensMan), client.getEntityId());
		AccessToken ac = resp1.getAccessToken();

		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL("https://localhost:52443/jwt-int/token"));
		httpReqRaw.setAuthorization("Bearer " + ac.getValue() + ",Basic Y2xpZW50QTpwYXNzd29yZA==");

		HTTPRequest httpReq = new HttpRequestConfigurer().secureRequest(httpReqRaw, new BinaryCertChainValidator(true),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse response = httpReq.send();
		Assert.assertEquals(200, response.getStatusCode());
	}

	@Test
	public void shouldFailWhenOnlyToken() throws Exception
	{
		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(),
				OAuthTestUtils.getOAuthProcessor(tokensMan), client.getEntityId());
		AccessToken ac = resp1.getAccessToken();

		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL("https://localhost:52443/jwt-int/token"));
		httpReqRaw.setAuthorization("Bearer " + ac.getValue());

		HTTPRequest httpReq = new HttpRequestConfigurer().secureRequest(httpReqRaw, new BinaryCertChainValidator(true),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse response = httpReq.send();
		Assert.assertEquals(500, response.getStatusCode());
	}

	@Test
	public void shouldFailWhenOnlyClientCredential() throws Exception
	{
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL("https://localhost:52443/jwt-int/token"));
		httpReqRaw.setAuthorization("Basic Y2xpZW50QTpwYXNzd29yZA==");

		HTTPRequest httpReq = new HttpRequestConfigurer().secureRequest(httpReqRaw, new BinaryCertChainValidator(true),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse response = httpReq.send();
		Assert.assertEquals(500, response.getStatusCode());
	}

	@Test
	public void shouldFailWhenClientNotMatchToToken() throws Exception
	{
		createUsernameUser("client2", InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE, "client2", CRED_REQ_PASS);

		AuthorizationSuccessResponse resp1 = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(),
				OAuthTestUtils.getOAuthProcessor(tokensMan), client.getEntityId());
		AccessToken ac = resp1.getAccessToken();

		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL("https://localhost:52443/jwt-int/token"));
		httpReqRaw.setAuthorization("Bearer " + ac.getValue() + ",Basic Y2xpZW50MjpjbGllbnQy");

		HTTPRequest httpReq = new HttpRequestConfigurer().secureRequest(httpReqRaw, new BinaryCertChainValidator(true),
				ServerHostnameCheckingMode.NONE);
		HTTPResponse response = httpReq.send();
		Assert.assertEquals(500, response.getStatusCode());
	}
}
