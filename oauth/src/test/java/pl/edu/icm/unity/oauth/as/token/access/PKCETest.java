/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.google.common.collect.Lists;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthScope;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.TestTxRunner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;


public class PKCETest 
{
	private TransactionalRunner tx = new TestTxRunner();

	@Test
	public void shouldFailToGetAccessTokenWithoutCodeVerifierWhenClientIsPublic() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext();
		OAuthAuthzContext ctx = createContextWithoutPKCE(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);
		ctx.setClientType(ClientType.PUBLIC);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, null, null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}
	

	
	@Test
	public void shouldFailToGetAccessTokenWithoutCodeVerifierWhenChallengeSet() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext();
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				"verifier__123456789012345678901234567890123", 
				CodeChallengeMethod.S256);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, null, null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}
	
	@Test
	public void shouldFailToGetAccessTokenWithWrongCodeVerifierWhenChallengeSet() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext();
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				"verifier__123456789012345678901234567890123", 
				CodeChallengeMethod.S256);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, 
				"WRONG_____123456789012345678901234567890123", null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldFailToAcceptTooShortChallenge() throws Exception
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		setupInvocationContext();
		String verifier = "TOOSHORT";
		createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				verifier, CodeChallengeMethod.S256);
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldFailToAcceptTooLongChallenge() throws Exception
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		setupInvocationContext();
		String verifier = "TOOLONG________________________________________________________________________"
				+ "__________________________________________________";
		createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				verifier, CodeChallengeMethod.S256);
	}

	@Test
	public void shouldSucceedWithProperPlainVerifier() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext();
		String verifier = "verifier__123456789012345678901234567890123";
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				verifier, CodeChallengeMethod.PLAIN);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, 
				verifier, null);
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}

	@Test
	public void shouldSucceedWithProperS256Verifier() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext();
		String verifier = "verifier__123456789012345678901234567890123";
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				verifier, CodeChallengeMethod.S256);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, 
				verifier, null);
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}

	private AccessTokenResource createAccessTokenResource(TokensManagement tokensManagement, OAuthASProperties config,
			TransactionalRunner tx)
	{
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));

		ClientAttributesProvider clientAttributesProvider = new ClientAttributesProvider(null);
		TokenService tokenUtils = new TokenService(null, config, null, clientAttributesProvider);
		OAuthTokenStatisticPublisher publisher = new OAuthTokenStatisticPublisher(mock(ApplicationEventPublisher.class),
				null, null, null, null, mock(LastIdPClinetAccessAttributeManagement.class), null, config,
				OAuthTestUtils.getEndpoint());

		AuthzCodeHandler authzCodeHandler = new AuthzCodeHandler(tokensManagement, accessTokenRepository,
				refreshTokenRepository, tx, new AccessTokenFactory(config), publisher, config, tokenUtils);
		RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(config, refreshTokenRepository, null,
				accessTokenRepository, null, null);
		ExchangeTokenHandler exchangeTokenHandler = new ExchangeTokenHandler(config, refreshTokenRepository, null,
				accessTokenRepository, null, null, null, null, null);
		CredentialFlowHandler credentialFlowHandler = new CredentialFlowHandler(config, null, null, null,
				accessTokenRepository, null);
		
		return new AccessTokenResource(authzCodeHandler, refreshTokenHandler, exchangeTokenHandler,
				credentialFlowHandler, null);

	}
	
	private void setupInvocationContext()
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow ,1, 1000);
		InvocationContext virtualAdmin = new InvocationContext(null, realm, Collections.emptyList());
		virtualAdmin.setLocale(Locale.ENGLISH);
		InvocationContext.setCurrent(virtualAdmin);
	}
	
	private static OAuthAuthzContext createContext(OAuthASProperties config, 
			ResponseType respType, GrantFlow grant, 
			long clientEntityId,
			String codeVerifier, CodeChallengeMethod method) throws Exception
	{
		AuthenticationRequest request = new AuthenticationRequest.Builder(respType, new Scope("openid"),
					new ClientID("clientC"), new URI("https://return.host.com/foo"))
				.nonce(new Nonce("nonce"))
				.codeChallenge(new CodeVerifier(codeVerifier), method)
				.build(); 
				
		return createContextFromRequest(request, config, grant, clientEntityId);
	}

	private static OAuthAuthzContext createContextWithoutPKCE(OAuthASProperties config, 
			ResponseType respType, GrantFlow grant, 
			long clientEntityId) throws Exception
	{
		AuthenticationRequest request = new AuthenticationRequest.Builder(respType, new Scope("openid"),
					new ClientID("clientC"), new URI("https://return.host.com/foo"))
				.nonce(new Nonce("nonce"))
				.build(); 

		return createContextFromRequest(request, config, grant, clientEntityId);
	}
	
	private static OAuthAuthzContext createContextFromRequest(AuthenticationRequest request, OAuthASProperties config, 
			GrantFlow grant, long clientEntityId) throws Exception
	{
		OAuthAuthzContext ctx = new OAuthAuthzContext(request, config);
		ctx.setClientEntityId(clientEntityId);
		ctx.setClientUsername("clientC");
		ctx.setFlow(grant);
		ctx.setOpenIdMode(false);
		ctx.setReturnURI(new URI("https://return.host.com/foo"));
		ctx.addEffectiveScopeInfo(OAuthScope.builder().withName("sc1").withDescription("scope 1")
				.withAttributes(Lists.newArrayList("email")).withEnabled(true).build());
		return ctx;
	}
}
