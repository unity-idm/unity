/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.junit.Test;

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

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.ScopeInfo;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.AccessTokenResource;
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
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, config, null, null, null, tx);
		setupInvocationContext(111);
		OAuthAuthzContext ctx = createContextWithoutPKCE(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);
		ctx.setClientType(ClientType.PUBLIC);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement, ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}
	

	
	@Test
	public void shouldFailToGetAccessTokenWithoutVodeVerifierWhenChallengeSet() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, config, null, null, null, tx);
		setupInvocationContext(111);
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				"verifier__123456789012345678901234567890123", 
				CodeChallengeMethod.S256);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement, ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}
	
	@Test
	public void shouldFailToGetAccessTokenWithWrongVodeVerifierWhenChallengeSet() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, config, null, null, null, tx);
		setupInvocationContext(111);
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				"verifier__123456789012345678901234567890123", 
				CodeChallengeMethod.S256);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement, ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, 
				"WRONG-VERIFIER");
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldFailToAcceptTooShortChallenge() throws Exception
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		setupInvocationContext(111);
		String verifier = "TOOSHORT";
		createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				verifier, CodeChallengeMethod.S256);
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldFailToAcceptTooLongChallenge() throws Exception
	{
		OAuthASProperties config = OAuthTestUtils.getConfig();
		setupInvocationContext(111);
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
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, config, null, null, null, tx);
		setupInvocationContext(111);
		String verifier = "verifier__123456789012345678901234567890123";
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				verifier, CodeChallengeMethod.PLAIN);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement, ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, 
				verifier);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}

	@Test
	public void shouldSucceedWithProperS256Verifier() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, config, null, null, null, tx);
		setupInvocationContext(111);
		String verifier = "verifier__123456789012345678901234567890123";
		OAuthAuthzContext ctx = createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100,
				verifier, CodeChallengeMethod.S256);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement, ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null, null, null, null, null, 
				verifier);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}

	
	private void setupInvocationContext(long entityId)
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow ,1, 1000);
		InvocationContext virtualAdmin = new InvocationContext(null, realm, Collections.emptyList());
		LoginSession loginSession = new LoginSession("sid", new Date(), 1000, entityId, "foo", null, null, null);
		virtualAdmin.setLoginSession(loginSession);
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
		ctx.addEffectiveScopeInfo(new ScopeInfo("sc1", "scope 1", Lists.newArrayList("email")));
		return ctx;
	}
}
