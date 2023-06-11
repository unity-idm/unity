/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.introspection;

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
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;

public class LocalTokenIntrospectionTest
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, LocalTokenIntrospectionTest.class);
	
	@Test
	public void shouldReturnInfoOnValidAccessToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		OAuthASProperties config = OAuthTestUtils.getConfig();
		LocalTokenIntrospectionService tested = createIntrospectionResource(tokensManagement);
		setupInvocationContext(111);
		AuthorizationSuccessResponse step1Resp = OAuthTestUtils.initOAuthFlowHybrid(config, 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
	
		Response r = tested.processLocalIntrospection(step1Resp.getAccessToken().getValue());
		
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
		assertThat(parsed.get("aud")).isEqualTo("clientC");
		assertThat(parsed.getAsString("iss")).isEqualTo(OAuthTestUtils.ISSUER);
	}
	
	@Test
	public void shouldReturnNotActiveOnUnknownToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		LocalTokenIntrospectionService tested = createIntrospectionResource(tokensManagement);
		setupInvocationContext(111);
		
		Response r = tested.processLocalIntrospection("UNKNOWN-TOKEN");
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
	
	private LocalTokenIntrospectionService createIntrospectionResource(TokensManagement tokensManagement)
	{
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		
		return new LocalTokenIntrospectionService(accessTokenRepository, refreshTokenRepository);
	}
}
