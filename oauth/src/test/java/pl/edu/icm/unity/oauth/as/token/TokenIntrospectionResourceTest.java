/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.AccessTokenFormat;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.OAuthTokenRepository;
import pl.edu.icm.unity.oauth.as.TestTxRunner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class TokenIntrospectionResourceTest
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenIntrospectionResourceTest.class);
	
	@Test
	public void shouldReturnInfoOnValidAccessToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		TokenIntrospectionResource tested = createIntrospectionResource(tokensManagement);
		setupInvocationContext(111);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		Response r = tested.introspectToken(step1Resp.getAccessToken().getValue());
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
		JSONObject parsed = (JSONObject) JSONValue.parse((r.getEntity().toString()));
		log.info("{}", parsed);
		assertThat(parsed.getAsString("active")).isEqualTo("true");
		assertThat(parsed.getAsString("scope")).isEqualTo("sc1");
		assertThat(parsed.getAsString("client_id")).isEqualTo("clientC");
		assertThat(parsed.getAsString("token_type")).isEqualTo("bearer");
		assertThat(parsed.getAsNumber("exp")).isEqualTo(
				parsed.getAsNumber("iat").intValue() + OAuthTestUtils.DEFAULT_ACCESS_TOKEN_VALIDITY);
		assertThat(parsed.getAsString("nbf")).isEqualTo(parsed.getAsString("iat"));
		assertThat(parsed.getAsString("sub")).isEqualTo("userA");
		assertThat(parsed.getAsString("aud")).isEqualTo("clientC");
		assertThat(parsed.getAsString("iss")).isEqualTo(OAuthTestUtils.ISSUER);
	}

	@Test
	public void shouldReturnInfoOnValidJWTAccessToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getOIDCConfig();
		config.setProperty(OAuthASProperties.ACCESS_TOKEN_FORMAT, AccessTokenFormat.JWT.toString());
		TokenIntrospectionResource tested = createIntrospectionResource(tokensManagement);
		setupInvocationContext(111);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		Response r = tested.introspectToken(step1Resp.getAccessToken().getValue());
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
		JSONObject parsed = (JSONObject) JSONValue.parse((r.getEntity().toString()));
		log.info("{}", parsed);
		assertThat(parsed.getAsString("active")).isEqualTo("true");
		assertThat(parsed.getAsString("scope")).isEqualTo("sc1");
		assertThat(parsed.getAsString("client_id")).isEqualTo("clientC");
		assertThat(parsed.getAsString("token_type")).isEqualTo("bearer");
		assertThat(parsed.getAsNumber("exp")).isEqualTo(
				parsed.getAsNumber("iat").intValue() + OAuthTestUtils.DEFAULT_ACCESS_TOKEN_VALIDITY);
		assertThat(parsed.getAsString("nbf")).isEqualTo(parsed.getAsString("iat"));
		assertThat(parsed.getAsString("sub")).isEqualTo("userA");
		assertThat(parsed.getAsString("aud")).isEqualTo("clientC");
		assertThat(parsed.getAsString("iss")).isEqualTo(OAuthTestUtils.ISSUER);
	}
	
	@Test
	public void shouldReturnInfoOnValidRefreshToken() throws Exception
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
				new OAuthTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class)), config, null, null, null, tx);
		Response resp = tokenEndpoint.getToken(GrantType.AUTHORIZATION_CODE.getValue(), 
				step1Resp.getAuthorizationCode().getValue(), null, "https://return.host.com/foo", 
				null, null, null, null, null, null, null);

		HTTPResponse httpResp = new HTTPResponse(resp.getStatus());
		httpResp.setContent(resp.getEntity().toString());
		httpResp.setContentType("application/json");
		OIDCTokenResponse tokensResponse = OIDCTokenResponse.parse(httpResp);
		
		
		TokenIntrospectionResource tested = createIntrospectionResource(tokensManagement);
		Response r = tested.introspectToken(tokensResponse.getTokens().getRefreshToken().getValue());
		
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
		JSONObject parsed = (JSONObject) JSONValue.parse((r.getEntity().toString()));
		log.info("{}", parsed);
		assertThat(parsed.getAsString("active")).isEqualTo("true");
		assertThat(parsed.getAsString("scope")).isEqualTo("sc1");
		assertThat(parsed.getAsString("client_id")).isEqualTo("clientC");
		assertThat(parsed.getAsString("token_type")).isEqualTo("bearer");
		assertThat(parsed.getAsNumber("exp")).isEqualTo(
				parsed.getAsNumber("iat").intValue() + 3600);
		assertThat(parsed.getAsString("nbf")).isEqualTo(parsed.getAsString("iat"));
		assertThat(parsed.getAsString("sub")).isEqualTo("userA");
		assertThat(parsed.getAsString("aud")).isEqualTo("clientC");
		assertThat(parsed.getAsString("iss")).isEqualTo(OAuthTestUtils.ISSUER);
	}

	
	@Test
	public void shouldReturnNotActiveOnUnknownToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		TokenIntrospectionResource tested = createIntrospectionResource(tokensManagement);
		setupInvocationContext(111);
		
		Response r = tested.introspectToken("UNKNOWN-TOKEN");
		assertEquals(HTTPResponse.SC_OK, r.getStatus());
		JSONObject parsed = (JSONObject) JSONValue.parse((r.getEntity().toString()));
		log.info("{}", parsed);
		assertThat(parsed.getAsString("active")).isEqualTo("false");
		assertThat(parsed.size()).isEqualTo(1);

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
	
	private TokenIntrospectionResource createIntrospectionResource(TokensManagement tokensManagement)
	{
		return new TokenIntrospectionResource(tokensManagement, new OAuthTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class)));
	}
}
