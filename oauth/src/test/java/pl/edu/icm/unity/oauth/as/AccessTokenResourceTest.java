/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.google.common.collect.Sets;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.token.AccessTokenResource;
import pl.edu.icm.unity.oauth.as.token.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.oauth.as.token.ClientTokensCleaner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class AccessTokenResourceTest
{
	private TransactionalRunner tx = new TestTxRunner();

	@Test
	public void gettingAccessTokenFailsWithWrongClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(111);
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(),
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", null, null, null,
				null, null, null, null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}

	@Test
	public void allGrantsExceptCodeAreFailingWithoutAuthentication() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);

		InvocationContext notAuthed = new InvocationContext(null, realm, Collections.emptyList());
		InvocationContext.setCurrent(notAuthed);

		OAuthAuthzContext ctx = OAuthTestUtils.createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		for (GrantType grant : new GrantType[]
		{ GrantType.CLIENT_CREDENTIALS, GrantType.TOKEN_EXCHANGE, GrantType.REFRESH_TOKEN })
		{
			Response r = tested.getToken(grant.getValue(), step1Resp.getAuthorizationCode().getValue(), null,
					"https://return.host.com/foo", null, null, null, null, null, null, null);
			assertEquals(HTTPResponse.SC_UNAUTHORIZED, r.getStatus());
		}
	}

	@Test
	public void gettingAccessTokenFailsWithWrongRedirect() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(100);

		OAuthAuthzContext ctx = OAuthTestUtils.createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);

		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		Response r = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(),
				step1Resp.getAuthorizationCode().getValue(), null, "https://wrong.com", null, null, null, null, null,
				null, null);
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
	}

	@Test
	public void gettingAccessTokenFailsOnInvalidToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(100);

		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(), "1234", null,
				"https://return.host.com/foo", null, null, null, null, null, null, null);
		assertEquals(400, resp.getStatus());
		JSONObject ret = (JSONObject) JSONValue.parse(resp.getEntity().toString());
		assertEquals("invalid_grant", ret.get("error"));
	}

	@Test
	public void accessTokenIsReturnedWithValidCodeWithOIDC() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(100);
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(),
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", null, null, null,
				null, null, null, null);

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

	@Test
	public void accessTokenIsReturnedWithEffectiveScopesIfAreOtherThenRequested() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(100);
		OAuthAuthzContext ctx = OAuthTestUtils.createContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100);
		ctx.setRequestedScopes(Sets.newHashSet("sc1", "scMissing"));

		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(),
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", null, null, null,
				null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		AccessTokenResponse parsed = AccessTokenResponse.parse(httpResp);
		AccessToken accessToken = parsed.getTokens().getAccessToken();
		assertThat(accessToken.getScope(), is(notNullValue()));
		assertThat(accessToken.getScope().contains("sc1"), is(true));
		assertThat(accessToken.getScope().size(), is(1));
	}

	@Test
	public void refreshTokenPresentIfConfigured() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, RefreshTokenIssuePolicy.ALWAYS.toString());
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_VALIDITY, "3600");

		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(100);
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(),
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", null, null, null,
				null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse parsed = OIDCTokenResponse.parse(httpResp);
		assertNotNull(parsed.getTokens().getRefreshToken());
	}

	@Test
	public void refreshTokenIsNotPresentIfConfigured() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, RefreshTokenIssuePolicy.NEVER.toString());

		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(100);
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(),
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", null, null, null,
				null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse parsed = OIDCTokenResponse.parse(httpResp);
		assertNull(parsed.getTokens().getRefreshToken());
	}

	@Test
	public void refreshTokenHasUnlimitedLifetimeIfConfiguredToZero() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, RefreshTokenIssuePolicy.ALWAYS.toString());
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_VALIDITY, "0");

		AccessTokenResource tested = createAccessTokenResource(tokensManagement, config, tx);
		setupInvocationContext(100);
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);

		Response resp = tested.getToken(GrantType.AUTHORIZATION_CODE.getValue(),
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", null, null, null,
				null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse parsed = OIDCTokenResponse.parse(httpResp);
		assertNotNull(parsed.getTokens().getRefreshToken());

		Token refreshTokenInternal = tokensManagement.getTokenById(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN,
				parsed.getTokens().getRefreshToken().getValue());
		assertThat(refreshTokenInternal.getExpires(), is(nullValue()));
	}

	private AccessTokenResource createAccessTokenResource(TokensManagement tokensManagement, OAuthASProperties config,
			TransactionalRunner tx)
	{
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		
		return new AccessTokenResource(tokensManagement, accessTokenRepository, refreshTokenRepository, new ClientTokensCleaner(accessTokenRepository, refreshTokenRepository),
				config, null, null, null, tx, mock(ApplicationEventPublisher.class), null, null, mock(LastIdPClinetAccessAttributeManagement.class), OAuthTestUtils.getEndpoint());
	}

	private void setupInvocationContext(long entityId)
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow, 1, 1000);
		InvocationContext virtualAdmin = new InvocationContext(null, realm, Collections.emptyList());
		LoginSession loginSession = new LoginSession("sid", new Date(), 1000, entityId, "foo", null, null, null);
		virtualAdmin.setLoginSession(loginSession);
		virtualAdmin.setLocale(Locale.ENGLISH);
		InvocationContext.setCurrent(virtualAdmin);
	}
}
