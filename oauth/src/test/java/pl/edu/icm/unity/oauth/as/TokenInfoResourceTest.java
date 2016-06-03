/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static pl.edu.icm.unity.oauth.as.OAuthProcessor.INTERNAL_ACCESS_TOKEN;

import java.util.Date;

import javax.ws.rs.core.Response;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.token.TokenInfoResource;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;

public class TokenInfoResourceTest
{
	@Test
	public void userInfoFailsOnInvalidToken() throws JsonProcessingException, EngineException, ParseException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		TokenInfoResource tested = new TokenInfoResource(tokensManagement);
		
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
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(tokensManagement);
		
		TokenInfoResource tested = new TokenInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		Response resp = tested.getToken(token);
		
		System.out.println(resp.getEntity().toString());
		JSONObject parsed = (JSONObject) JSONValue.parse((resp.getEntity().toString()));
		assertEquals("userA", parsed.get("sub"));
		assertEquals("clientC", parsed.get("client_id"));
		assertEquals("sc1", ((JSONArray)parsed.get("scope")).get(0));
		assertNotNull(parsed.get("exp"));
	}

	
	@Test
	public void tokenValidityIsEnhancedOnRequest() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(tokensManagement, 100, 1000);
		
		TokenInfoResource tested = new TokenInfoResource(tokensManagement);
		
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
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(tokensManagement, 100, 101);
		
		TokenInfoResource tested = new TokenInfoResource(tokensManagement);
		
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
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(tokensManagement, 100, 0);
		
		TokenInfoResource tested = new TokenInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		
		Token tokenBefore = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		Date initialExpiry = tokenBefore.getExpires();
		Thread.sleep(50);
		tested.getToken(token);
		Token tokenAfter = tokensManagement.getTokenById(INTERNAL_ACCESS_TOKEN, respInit.getAccessToken().getValue());
		
		assertThat(initialExpiry.getTime(), is(tokenAfter.getExpires().getTime()));
	}
	
}
