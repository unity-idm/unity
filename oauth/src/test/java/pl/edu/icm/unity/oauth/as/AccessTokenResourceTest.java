/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.AccessTokenResource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class AccessTokenResourceTest 
{
	private TransactionalRunner tx = new TransactionalRunner()
	{
		@Override
		public <T> T runInTransactionRet(TxRunnableRet<T> code)
		{
			return code.run();
		}
		
		@Override
		public void runInTransaction(TxRunnable code)
		{
			code.run();
		}

		@Override
		public void runInTransactionNoAutoCommit(TxRunnable code)
		{
			code.run();
		}

		@Override
		public <T> T runInTransactionNoAutoCommitRet(TxRunnableRet<T> code)
		{
			return code.run();
		}

		@Override
		public void runInTransactionThrowing(TxRunnableThrowing code) throws EngineException
		{
			code.run();
		}

		@Override
		public <T> T runInTransactionRetThrowing(TxRunnableThrowingRet<T> code)
				throws EngineException
		{
			return code.run();
		}
	};
	
	@Test
	public void userInfoFailsWithWrongClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, null, config, null, null, tx);
		setupInvocationContext(111);
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement,
				ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), 
				null,
				"https://return.host.com/foo",
				null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}
	
	@Test
	public void userInfoFailsWithWrongRedirect() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, null, config, null, null, tx);
		setupInvocationContext(100);

		OAuthAuthzContext ctx = OAuthTestUtils.createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);

		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement,
				ctx);
		
		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(),
				null,
				"https://wrong.com",
				null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}
	
	@Test
	public void userInfoFailsOnInvalidToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, null, config, null, null, tx);
		setupInvocationContext(100);

		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				"1234", null, "https://return.host.com/foo", null);
		assertEquals(400, resp.getStatus());
		JSONObject ret = (JSONObject) JSONValue.parse(resp.getEntity().toString());
		assertEquals("invalid_grant", ret.get("error"));
	}
	
	@Test
	public void userInfoWorksWithValidToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = new AccessTokenResource(tokensManagement, null, config, null, null, tx);
		setupInvocationContext(100);
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, 
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(tokensManagement,
				ctx);
		
		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse parsed = OIDCTokenResponse.parse(httpResp);
		assertNotNull(parsed.getOIDCTokens().getAccessToken());
		assertNotNull(parsed.getOIDCTokens().getIDToken());
		JWTClaimsSet idToken = parsed.getOIDCTokens().getIDToken().getJWTClaimsSet();
		assertEquals("userA", idToken.getSubject());
		assertTrue(idToken.getAudience().contains("clientC"));
		assertEquals(OAuthTestUtils.ISSUER, idToken.getIssuer());
		
	}

	private void setupInvocationContext(long entityId)
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, -1, 1000);
		InvocationContext virtualAdmin = new InvocationContext(null, realm);
		LoginSession loginSession = new LoginSession("sid", new Date(), 1000, entityId, "foo");
		virtualAdmin.setLoginSession(loginSession);
		virtualAdmin.setLocale(Locale.ENGLISH);
		InvocationContext.setCurrent(virtualAdmin);
	}
}
