/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;

import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.OAuthTokenRepository;
import pl.edu.icm.unity.oauth.as.TestTxRunner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class RevocationResourceTest
{
	@Test
	public void shouldRevokeAccessTokenWithoutHint() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(111);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), "clientC", null, null);
		
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}
	
	@Test
	public void shouldRevokeRefreshTokenWithoutHint() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_VALIDITY, "3600");
		setupInvocationContext(100);

		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, 
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
		TransactionalRunner tx = new TestTxRunner();
		AccessTokenResource tokenEndpoint = new AccessTokenResource(tokensManagement,
				new OAuthTokenRepository(tokensManagement, mock(SecuredTokensManagement.class)), config, null, null,
				null, tx, mock(ApplicationEventPublisher.class), null, mock(EndpointManagement.class), 
				OAuthTestUtils.getEndpoint());
		Response resp = tokenEndpoint.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", 
				null, null, null, null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse tokensResponse = OIDCTokenResponse.parse(httpResp);
		
		RevocationResource tested = createRevocationResource(tokensManagement);
		Response r = tested.revoke(tokensResponse.getTokens().getRefreshToken().getValue(), "clientC", null, null);

		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}
	
	@Test
	public void shouldRejectRevocationWithoutClientIdForPublicClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(111);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), null, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		//FIXME
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}

	@Test
	public void shouldRejectRevocationWithWrongClientIdForConfidentialClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(111);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), null, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		//FIXME
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}

	@Test
	public void shouldRevokeWithoutClientIdForConfidentialClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(111);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), null, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		//FIXME
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
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
	
	private RevocationResource createRevocationResource(TokensManagement tokensManagement)
	{
		return new RevocationResource(tokensManagement, 
				new OAuthTokenRepository(tokensManagement, mock(SecuredTokensManagement.class)),
				mock(SessionManagement.class),
				new AuthenticationRealm(),
				"/oauth-clients");
	}
}
