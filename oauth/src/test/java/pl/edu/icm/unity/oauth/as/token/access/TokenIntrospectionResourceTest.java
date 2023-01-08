/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockPKIMan;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.TestTxRunner;
import pl.edu.icm.unity.oauth.as.token.introspection.LocalTokenIntrospectionService;
import pl.edu.icm.unity.oauth.as.token.introspection.RemoteTokenIntrospectionService;
import pl.edu.icm.unity.oauth.as.token.introspection.TokenIntrospectionResource;
import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class TokenIntrospectionResourceTest
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenIntrospectionResourceTest.class);

	@Test
	public void shouldRedirectToRemoteService() throws Exception
	{
		RemoteTokenIntrospectionService remoteTokenIntrospectionService = mock(RemoteTokenIntrospectionService.class);
		TokenIntrospectionResource res = new TokenIntrospectionResource(remoteTokenIntrospectionService, null,
				OAuthTestUtils.ISSUER);
		SignedJWT jwts = SignedJWT.parse(
				JWTUtils.generate(new MockPKIMan().getCredential("MAIN"), new JWTClaimsSet.Builder().issuer("rem")
						.build()));
		res.introspectToken(jwts.serialize());
		verify(remoteTokenIntrospectionService).processRemoteIntrospection(any());
	}
	
	@Test
	public void shouldIntrospectByLocalService() throws Exception
	{
		LocalTokenIntrospectionService localTokenIntrospectionService = mock(LocalTokenIntrospectionService.class);
		TokenIntrospectionResource res = new TokenIntrospectionResource(null, localTokenIntrospectionService,
				OAuthTestUtils.ISSUER);
		SignedJWT jwts = SignedJWT.parse(
				JWTUtils.generate(new MockPKIMan().getCredential("MAIN"), new JWTClaimsSet.Builder().issuer(OAuthTestUtils.ISSUER)
						.build()));
		res.introspectToken(jwts.serialize());
		verify(localTokenIntrospectionService).processLocalIntrospection(any());
	}

	@Test
	public void shouldReturnInfoOnValidLocalRefreshToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_VALIDITY, "3600");
		config.setProperty(OAuthASProperties.REFRESH_TOKEN_ISSUE_POLICY, RefreshTokenIssuePolicy.ALWAYS.toString());

		setupInvocationContext(100);

		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(config, new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils
				.initOAuthFlowAccessCode(OAuthTestUtils.getOAuthProcessor(tokensManagement), ctx);
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

		AccessTokenResource tokenEndpoint = new AccessTokenResource(authzCodeHandler, refreshTokenHandler,
				exchangeTokenHandler, credentialFlowHandler, null);

		Response resp = tokenEndpoint.getToken(GrantType.AUTHORIZATION_CODE.getValue(), step1Resp.getAuthorizationCode()
				.getValue(), null, "https://return.host.com/foo", null, null, null, null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity()
				.toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse tokensResponse = OIDCTokenResponse.parse(httpResp);

		TokenIntrospectionResource tested = createIntrospectionResource(tokensManagement);
		Response r = tested.introspectToken(tokensResponse.getTokens()
				.getRefreshToken()
				.getValue());

		assertEquals(HTTPResponse.SC_OK, r.getStatus());
		JSONObject parsed = (JSONObject) JSONValue.parse((r.getEntity()
				.toString()));
		log.info("{}", parsed);
		assertThat(parsed.getAsString("active")).isEqualTo("true");
		assertThat(parsed.getAsString("scope")).isEqualTo("sc1");
		assertThat(parsed.getAsString("client_id")).isEqualTo("clientC");
		assertThat(parsed.getAsString("token_type")).isEqualTo("bearer");
		assertThat(parsed.getAsNumber("exp")).isEqualTo(parsed.getAsNumber("iat")
				.intValue() + 3600);
		assertThat(parsed.getAsString("nbf")).isEqualTo(parsed.getAsString("iat"));
		assertThat(parsed.getAsString("sub")).isEqualTo("userA");
		assertThat(parsed.get("aud")).isEqualTo("clientC");
		assertThat(parsed.getAsString("iss")).isEqualTo(OAuthTestUtils.ISSUER);
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

	private TokenIntrospectionResource createIntrospectionResource(TokensManagement tokensManagement)
	{
		RemoteTokenIntrospectionService remoteTokenIntrospectionService = mock(RemoteTokenIntrospectionService.class);
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement,
				mock(SecuredTokensManagement.class));

		return new TokenIntrospectionResource(remoteTokenIntrospectionService,
				new LocalTokenIntrospectionService(accessTokenRepository, refreshTokenRepository),
				OAuthTestUtils.ISSUER);
	}
}
