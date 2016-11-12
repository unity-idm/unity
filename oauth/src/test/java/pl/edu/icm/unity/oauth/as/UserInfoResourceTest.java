/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static pl.edu.icm.unity.oauth.as.OAuthProcessor.INTERNAL_ACCESS_TOKEN;

import java.util.Date;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.token.UserInfoResource;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

public class UserInfoResourceTest
{
	@Test
	public void userInfoFailsOnInvalidToken() throws JsonProcessingException, EngineException, ParseException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		UserInfoResource tested = new UserInfoResource(tokensManagement);
		
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
				tokensManagement);
		
		UserInfoResource tested = new UserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		Response resp = tested.getToken(token);
		assertEquals("application/json", resp.getHeaderString("Content-Type"));
		UserInfo parsed = UserInfo.parse(resp.getEntity().toString());
		assertEquals("userA", parsed.getSubject().getValue());
		assertEquals("example@example.com", parsed.getEmail().getAddress());
	}
	
	@Test
	public void tokenValidityIsEnhancedOnRequest() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(
				OAuthTestUtils.getConfig(100, 1000), 
				tokensManagement);
		
		UserInfoResource tested = new UserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		
		Token tokenBefore = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		Date initialExpiry = tokenBefore.getExpires();
		Thread.sleep(50);
		tested.getToken(token);
		Token tokenAfter = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		
		assertThat(initialExpiry.before(tokenAfter.getExpires()), is(true));
	}

	@Test
	public void tokenValidityIsNeverExceedsMaximum() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(
				OAuthTestUtils.getConfig(100, 101), 
				tokensManagement);
		
		UserInfoResource tested = new UserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		
		Token tokenBefore = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		Date initialExpiry = tokenBefore.getExpires();
		Thread.sleep(1010);
		tested.getToken(token);
		Token tokenAfter = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		
		assertThat(initialExpiry.before(tokenAfter.getExpires()), is(true));
		assertThat(tokenAfter.getExpires().getTime(), is(tokenAfter.getCreated().getTime() + 101000));
	}
	
	@Test
	public void tokenValidityIsNotEnhancedOnRequestWhenMaxIsDisabled() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(100, 0), 
				tokensManagement);
		
		UserInfoResource tested = new UserInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		
		Token tokenBefore = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		Date initialExpiry = tokenBefore.getExpires();
		Thread.sleep(50);
		tested.getToken(token);
		Token tokenAfter = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		
		assertThat(initialExpiry.getTime(), is(tokenAfter.getExpires().getTime()));
	}
}
