/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.oauth.as.OAuthTestUtils.TOKEN_OWNING_CLIENT_CLIENT_ID;
import static pl.edu.icm.unity.oauth.as.OAuthTestUtils.TOKEN_OWNING_CLIENT_ENTITY_ID;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.TestTxRunner;
import pl.edu.icm.unity.oauth.as.token.RevocationResource;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class RevocationResourceAuthnHintTest
{
	@After
	public void cleanup()
	{
		InvocationContext.setCurrent(null);
	}
	
	@Test
	public void shouldRevokeAccessTokenWithoutHint() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(TOKEN_OWNING_CLIENT_ENTITY_ID);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), TOKEN_OWNING_CLIENT_CLIENT_ID, null, null);
		
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}
	
	@Test
	public void shouldRevokeRefreshTokenWithoutHint() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_VALIDITY, "3600");
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, RefreshTokenIssuePolicy.ALWAYS.toString());

		setupInvocationContext(TOKEN_OWNING_CLIENT_ENTITY_ID);

		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, 
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, TOKEN_OWNING_CLIENT_ENTITY_ID, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowAccessCode(
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
		TransactionalRunner tx = new TestTxRunner();
		
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
		
		AccessTokenResource tokenEndpoint = new AccessTokenResource(authzCodeHandler, refreshTokenHandler, exchangeTokenHandler,
				credentialFlowHandler, null);
		Response resp = tokenEndpoint.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", 
				null, null, null, null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse tokensResponse = OIDCTokenResponse.parse(httpResp);
		
		RevocationResource tested = createRevocationResource(tokensManagement);
		Response r = tested.revoke(tokensResponse.getTokens().getRefreshToken().getValue(), TOKEN_OWNING_CLIENT_CLIENT_ID, null, null);

		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}
	
	@Test
	public void shouldRejectRevocationWithoutClientIdForPublicClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(TOKEN_OWNING_CLIENT_ENTITY_ID);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ClientType.PUBLIC);
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), null, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
		assertThat(r.readEntity(String.class)).contains("client_id must be provided");
	}

	@Test
	public void shouldRejectRevocationWithWrongClientIdForConfidentialClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(TOKEN_OWNING_CLIENT_ENTITY_ID);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ClientType.CONFIDENTIAL);
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), "wrong-client-id", RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertEquals(HTTPResponse.SC_UNAUTHORIZED, r.getStatus());
		assertThat(r.readEntity(String.class)).contains("Wrong client\\/token");
	}

	@Test
	public void shouldRevokeWithoutClientIdForConfidentialClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupInvocationContext(TOKEN_OWNING_CLIENT_ENTITY_ID);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ClientType.CONFIDENTIAL);
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), null, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}

	@Test
	public void shouldRevokeWithoutAuthnForConfidentialClientInLegacyMode() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		
		RevocationResource tested = new RevocationResource(accessTokenRepository, refreshTokenRepository, 
				mock(SessionManagement.class),
				new AuthenticationRealm(),
				true);
		setupNoAuthnInvocationContext();
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ClientType.CONFIDENTIAL);
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), TOKEN_OWNING_CLIENT_CLIENT_ID, 
				RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
	}

	@Test
	public void shouldRejectToRevokeWithoutAuthnForConfidentialClient() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		RevocationResource tested = createRevocationResource(tokensManagement);
		setupNoAuthnInvocationContext();
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement), ClientType.CONFIDENTIAL);
		
		Response r = tested.revoke(step1Resp.getAccessToken().getValue(), TOKEN_OWNING_CLIENT_CLIENT_ID, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertEquals(HTTPResponse.SC_BAD_REQUEST, r.getStatus());
		assertThat(r.readEntity(String.class)).contains("Authentication is required");
	}


	private void setupNoAuthnInvocationContext()
	{
		AuthenticationRealm realm = new AuthenticationRealm("foo", "", 5, 10, RememberMePolicy.disallow ,1, 1000);
		InvocationContext virtualAdmin = new InvocationContext(null, realm, Collections.emptyList());
		InvocationContext.setCurrent(virtualAdmin);
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
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		
		return new RevocationResource(accessTokenRepository, refreshTokenRepository, 
				mock(SessionManagement.class),
				new AuthenticationRealm(),
				false);
	}
}
