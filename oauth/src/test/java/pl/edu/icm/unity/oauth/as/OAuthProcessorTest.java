/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzContext;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityParam;

import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;

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
		Collection<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(new StringAttribute("email", "/", AttributeVisibility.full, "example@example.com"));
		IdentityParam identity = new IdentityParam("username", "userA");
		TokensManagement tokensMan = new MockTokensMan();
		OAuthAuthzContext ctx = createContext(new ResponseType(ResponseType.Value.CODE),
				GrantFlow.authorizationCode);
		
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
		Collection<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(new StringAttribute("email", "/", AttributeVisibility.full, "example@example.com"));
		IdentityParam identity = new IdentityParam("username", "userA");
		TokensManagement tokensMan = new MockTokensMan();
		OAuthAuthzContext ctx = createContext(new ResponseType(ResponseType.Value.TOKEN),
				GrantFlow.implicit);
		
		long start = System.currentTimeMillis();
		AuthorizationSuccessResponse resp = processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, tokensMan);
		long end = System.currentTimeMillis();
		
		assertNotNull(resp.getAccessToken());
		assertNull(resp.getAuthorizationCode());
		
		AccessToken accessToken = resp.getAccessToken();
		Token internalAccessToken = tokensMan.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
				accessToken.getValue());
		verifyToken(internalAccessToken, start, end, 100);		
	}
	
	@Test
	public void checkHybridFlowResponse() throws Exception
	{
		OAuthProcessor processor = new OAuthProcessor();
		Collection<Attribute<?>> attributes = new ArrayList<>();
		attributes.add(new StringAttribute("email", "/", AttributeVisibility.full, "example@example.com"));
		IdentityParam identity = new IdentityParam("username", "userA");
		TokensManagement tokensMan = new MockTokensMan();
		OAuthAuthzContext ctx = createContext(new ResponseType(ResponseType.Value.TOKEN, 
				OIDCResponseTypeValue.ID_TOKEN, ResponseType.Value.CODE),
				GrantFlow.openidHybrid);
		
		long start = System.currentTimeMillis();
		AuthorizationSuccessResponse resp = processor.prepareAuthzResponseAndRecordInternalState(
				attributes, identity, ctx, tokensMan);
		long end = System.currentTimeMillis();
		
		assertNotNull(resp.getAccessToken());
		assertNotNull(resp.getAuthorizationCode());
		
		AccessToken accessToken = resp.getAccessToken();
		Token internalAccessToken = tokensMan.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
				accessToken.getValue());
		verifyToken(internalAccessToken, start, end, 100);
		
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
		ReadOnlyJWTClaimsSet openidClaims = openidToken.getJWTClaimsSet();
		assertEquals("https://localhost:2443/oauth-as", openidClaims.getIssuer());
		assertEquals("userA", openidClaims.getSubject());
		assertEquals(1, openidClaims.getAudience().size());
		assertEquals("clientC", openidClaims.getAudience().get(0));
		assertTrue(openidClaims.getIssueTime().getTime() >= startS);
		assertTrue(openidClaims.getIssueTime().getTime() <= endS);
		assertTrue(openidClaims.getExpirationTime().getTime() >= startS+300000);
		assertTrue(openidClaims.getExpirationTime().getTime() <= endS+300000);
		
		assertNotNull(internalToken.getUserInfo());
		UserInfo userInfo = UserInfo.parse(internalToken.getUserInfo());
		assertEquals("example@example.com", userInfo.getEmail().getAddress());
		assertNotNull(userInfo.getSubject());
		assertEquals("userA", userInfo.getSubject().getValue());		
	}
	
	private OAuthAuthzContext createContext(ResponseType respType, GrantFlow grant) throws Exception
	{
		AuthorizationRequest request = new AuthorizationRequest(null, respType, 
				new ClientID("clientC"), null, null, new State("state123"));
		X509Credential credential = new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), null, "pkcs12");
		OAuthAuthzContext ctx = new OAuthAuthzContext(
				request, 
				100, 
				200, 
				300, 
				"https://localhost:2443/oauth-as", 
				credential);
		ctx.setClientEntityId(100);
		ctx.setClientName("clientC");
		ctx.setFlow(grant);
		ctx.setOpenIdMode(true);
		ctx.setReturnURI(new URI("https://return.host.com/foo"));
		return ctx;
	}
}
