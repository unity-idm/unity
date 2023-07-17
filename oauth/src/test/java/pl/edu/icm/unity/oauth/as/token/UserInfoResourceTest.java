/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository.INTERNAL_ACCESS_TOKEN;

import java.util.Date;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;

public class UserInfoResourceTest
{
	@Test
	public void userInfoFailsOnInvalidToken() throws JsonProcessingException, EngineException, ParseException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		UserInfoResource tested = createUserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken("missing").toAuthorizationHeader();
		Response resp = tested.getToken(token);
		BearerTokenError error = BearerTokenError.parse(resp.getHeaderString("WWW-Authenticate"));
		assertEquals(401, resp.getStatus());
		assertEquals("invalid_token", error.getCode());
	}
	
	@Test
	public void userInfoWorksWithValidToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(), 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		UserInfoResource tested = createUserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		Response resp = tested.getToken(token);
		assertEquals("application/json", resp.getHeaderString("Content-Type"));
		UserInfo parsed = UserInfo.parse(resp.getEntity().toString());
		assertEquals("userA", parsed.getSubject().getValue());
		assertEquals("example@example.com", parsed.getEmailAddress());
	}
	
	@Test
	public void tokenValidityIsEnhancedOnRequest() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(
				OAuthTestUtils.getConfig(100, 1000, false), 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		UserInfoResource tested = createUserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		
		Token tokenBefore = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		Date initialExpiry = tokenBefore.getExpires();
		Thread.sleep(50);
		tested.getToken(token);
		Token tokenAfter = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		
		assertThat(initialExpiry.before(tokenAfter.getExpires())).isTrue();
	}

	@Test
	public void tokenValidityIsNeverExceedsMaximum() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(
				OAuthTestUtils.getConfig(100, 101, false), 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		UserInfoResource tested = createUserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		
		Token tokenBefore = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		Date initialExpiry = tokenBefore.getExpires();
		Thread.sleep(1010);
		tested.getToken(token);
		Token tokenAfter = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		
		assertThat(initialExpiry.before(tokenAfter.getExpires())).isTrue();
		assertThat(tokenAfter.getExpires().getTime()).isEqualTo(tokenAfter.getCreated().getTime() + 101000);
	}
	
	@Test
	public void tokenValidityIsNotEnhancedOnRequestWhenMaxIsDisabled() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(100, 0, false), 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		UserInfoResource tested = createUserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		
		Token tokenBefore = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		Date initialExpiry = tokenBefore.getExpires();
		Thread.sleep(50);
		tested.getToken(token);
		Token tokenAfter = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		
		assertThat(initialExpiry.getTime()).isEqualTo(tokenAfter.getExpires().getTime());
	}
	
	private UserInfoResource createUserInfoResource(TokensManagement tokensManagement)
	{
		return new UserInfoResource(new OAuthAccessTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class)));
	}
}
