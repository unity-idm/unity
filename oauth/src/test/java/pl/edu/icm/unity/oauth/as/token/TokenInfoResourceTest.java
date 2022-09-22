/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthTestUtils;

public class TokenInfoResourceTest
{
	@Test
	public void tokenInfoFailsOnInvalidToken() throws JsonProcessingException, EngineException, ParseException
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		TokenInfoResource tested = createTokenInfoResource(tokensManagement);
		
		String token = new BearerAccessToken("missing").toAuthorizationHeader();
		Response resp = tested.getToken(token);
		BearerTokenError error = BearerTokenError.parse(resp.getHeaderString("WWW-Authenticate"));
		assertEquals(401, resp.getStatus());
		assertEquals("invalid_token", error.getCode());
	}
	
	@Test
	public void tokenInfoWorksWithValidToken() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		
		AuthorizationSuccessResponse respInit = OAuthTestUtils.initOAuthFlowHybrid(OAuthTestUtils.getConfig(), 
				OAuthTestUtils.getOAuthProcessor(tokensManagement));
		
		TokenInfoResource tested = createTokenInfoResource(tokensManagement);
		
		String token = new BearerAccessToken(respInit.getAccessToken().getValue()).toAuthorizationHeader();
		Response resp = tested.getToken(token);
		
		System.out.println(resp.getEntity().toString());
		JSONObject parsed = (JSONObject) JSONValue.parse((resp.getEntity().toString()));
		assertEquals("userA", parsed.get("sub"));
		assertEquals("clientC", parsed.get("client_id"));
		assertEquals("clientC", parsed.get("aud"));
		assertEquals("sc1", ((JSONArray)parsed.get("scope")).get(0));
		assertNotNull(parsed.get("exp"));
	}
	
	private TokenInfoResource createTokenInfoResource(TokensManagement tokensManagement)
	{
		return new TokenInfoResource(new OAuthAccessTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class)));
	}
}
