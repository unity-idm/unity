/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationSuccessResponse;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Tests of the authz endpoint logic.
 * @author K. Benedyczak
 */
public class OAuthProcessorTest
{
	@Test
	public void checkCodeFlowResponse() throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<DynamicAttribute> attributes = new ArrayList<>();
		attributes.add(new DynamicAttribute(StringAttribute.of("email", "/", "example@example.com")));
		IdentityParam identity = new IdentityParam("username", "userA");
		TokensManagement tokensMan = new MockTokensMan();
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode, 100, "nonce");
		
		long start = System.currentTimeMillis();
		AuthorizationSuccessResponse resp = processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, tokensMan);
		long end = System.currentTimeMillis();
		
		assertNull(resp.getAccessToken());
		assertNotNull(resp.getAuthorizationCode());
		AuthorizationCode authzCode = resp.getAuthorizationCode();
		Token codeToken = tokensMan.getTokenById(OAuthProcessor.INTERNAL_CODE_TOKEN, 
				authzCode.getValue().toString());
		verifyToken(codeToken, start, end, 200);
	}
	
	@Test
	public void checkImplicitFlowResponse() throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<DynamicAttribute> attributes = new ArrayList<>();
		attributes.add(new DynamicAttribute(StringAttribute.of("email", "/", "example@example.com")));
		IdentityParam identity = new IdentityParam("username", "userA");
		TokensManagement tokensMan = new MockTokensMan();
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN),
				GrantFlow.implicit, 100, "nonce");
		
		long start = System.currentTimeMillis();
		AuthorizationSuccessResponse resp = processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, tokensMan);
		long end = System.currentTimeMillis();
		
		assertNotNull(resp.getAccessToken());
		assertNull(resp.getAuthorizationCode());
		assertHasATHash((AuthenticationSuccessResponse) resp);
		
		AccessToken accessToken = resp.getAccessToken();
		Token internalAccessToken = tokensMan.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
				accessToken.getValue());
		verifyToken(internalAccessToken, start, end, 100);		
		assertHasATHash(internalAccessToken);
	}
	
	@Test
	public void checkHybridFlowResponse() throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<DynamicAttribute> attributes = new ArrayList<>();
		attributes.add(new DynamicAttribute(StringAttribute.of("email", "/", "example@example.com")));
		IdentityParam identity = new IdentityParam("username", "userA");
		TokensManagement tokensMan = new MockTokensMan();
		OAuthAuthzContext ctx = OAuthTestUtils.createOIDCContext(OAuthTestUtils.getConfig(),
				new ResponseType(ResponseType.Value.TOKEN, OIDCResponseTypeValue.ID_TOKEN, ResponseType.Value.CODE),
				GrantFlow.openidHybrid, 100, "nonce");
		
		long start = System.currentTimeMillis();
		AuthorizationSuccessResponse resp = processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, tokensMan);
		long end = System.currentTimeMillis();
		
		assertNotNull(resp.getAccessToken());
		assertNotNull(resp.getAuthorizationCode());
		assertHasATHash((AuthenticationSuccessResponse) resp);
		assertHasCHash((AuthenticationSuccessResponse) resp);
		
		AccessToken accessToken = resp.getAccessToken();
		Token internalAccessToken = tokensMan.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
				accessToken.getValue());
		verifyToken(internalAccessToken, start, end, 100);
		assertHasCHash(internalAccessToken);
		assertHasATHash(internalAccessToken);
		
		AuthorizationCode authzCode = resp.getAuthorizationCode();
		Token codeToken = tokensMan.getTokenById(OAuthProcessor.INTERNAL_CODE_TOKEN, 
				authzCode.getValue().toString());
		verifyToken(codeToken, start, end, 200);
	}
	
	private void verifyToken(Token token, long start, long end, int expiry) throws Exception
	{
		long startS = (start / 1000) * 1000;
		long endS = (end / 1000) * 1000;
		assertTrue(token.getCreated().getTime() >= start);
		assertTrue(token.getCreated().getTime() <= end);
		assertTrue(token.getExpires().getTime() + " " + (start+expiry*1000),
				token.getExpires().getTime() >= start+expiry*1000);
		assertTrue(token.getExpires().getTime() <= end+ expiry*1000);
		
		OAuthToken internalToken = OAuthToken.getInstanceFromJson(token.getContents());
		assertNotNull(internalToken.getOpenidInfo());
		SignedJWT openidToken = SignedJWT.parse(internalToken.getOpenidInfo());
		JWTClaimsSet openidClaims = openidToken.getJWTClaimsSet();
		assertEquals(OAuthTestUtils.ISSUER, openidClaims.getIssuer());
		assertEquals("userA", openidClaims.getSubject());
		assertEquals(1, openidClaims.getAudience().size());
		assertEquals("clientC", openidClaims.getAudience().get(0));
		assertTrue(openidClaims.getIssueTime().getTime() >= startS);
		assertTrue(openidClaims.getIssueTime().getTime() <= endS);
		assertTrue(openidClaims.getExpirationTime().getTime() >= startS+300000);
		assertTrue(openidClaims.getExpirationTime().getTime() <= endS+300000);
		
		assertNotNull(internalToken.getUserInfo());
		UserInfo userInfo = UserInfo.parse(internalToken.getUserInfo());
		assertEquals("example@example.com", userInfo.getEmailAddress());
		assertNotNull(userInfo.getSubject());
		assertEquals("userA", userInfo.getSubject().getValue());		
	}

	private void assertHasATHash(AuthenticationSuccessResponse response) throws ParseException, com.nimbusds.oauth2.sdk.ParseException
	{
		assertHasATHash(response.getIDToken().getJWTClaimsSet());
	}
	
	private void assertHasATHash(Token token) throws ParseException, com.nimbusds.oauth2.sdk.ParseException
	{
		OAuthToken internalToken = OAuthToken.getInstanceFromJson(token.getContents());
		SignedJWT openidToken = SignedJWT.parse(internalToken.getOpenidInfo());
		assertHasATHash(openidToken.getJWTClaimsSet());
	}

	private void assertHasATHash(JWTClaimsSet openidClaims) throws com.nimbusds.oauth2.sdk.ParseException
	{
		IDTokenClaimsSet idTokenClaimsSet = new IDTokenClaimsSet(openidClaims);
		assertThat(idTokenClaimsSet.getAccessTokenHash(), is(notNullValue()));
		assertThat(idTokenClaimsSet.getAccessTokenHash().getValue(), is(notNullValue()));
	}

	private void assertHasCHash(AuthenticationSuccessResponse response) throws ParseException, com.nimbusds.oauth2.sdk.ParseException
	{
		assertHasCHash(response.getIDToken().getJWTClaimsSet());
	}
	
	private void assertHasCHash(Token token) throws ParseException, com.nimbusds.oauth2.sdk.ParseException
	{
		OAuthToken internalToken = OAuthToken.getInstanceFromJson(token.getContents());
		SignedJWT openidToken = SignedJWT.parse(internalToken.getOpenidInfo());
		assertHasCHash(openidToken.getJWTClaimsSet());
	}

	private void assertHasCHash(JWTClaimsSet openidClaims) throws com.nimbusds.oauth2.sdk.ParseException
	{
		IDTokenClaimsSet idTokenClaimsSet = new IDTokenClaimsSet(openidClaims);
		
		assertThat(idTokenClaimsSet.getCodeHash(), is(notNullValue()));
		assertThat(idTokenClaimsSet.getCodeHash().getValue(), is(notNullValue()));
	}

}
