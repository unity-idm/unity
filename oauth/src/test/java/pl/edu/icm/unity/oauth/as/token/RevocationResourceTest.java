/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.oauth.as.InternalAccessTokenTestExposer.INTERNAL_ACCESS_TOKEN;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.as.token.access.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.EntityParam;

public class RevocationResourceTest
{	
	private static final long CLIENT_ENTITY_ID = 123l;
	private static final String CLIENT_ID = "clientId";

	@After
	public void cleanup()
	{
		InvocationContext.setCurrent(null);
	}
	
	@Test
	public void userIdMustMatchTokenOwner() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createAccessToken(tokensManagement);
		RevocationResource tested = createRevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("ac", "clientIdOther", RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_UNAUTHORIZED));
		assertThat(response.readEntity(String.class), containsString("invalid_client"));
	}
	
	@Test
	public void nonExistingTokenIsAccepted() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createAccessToken(tokensManagement);
		RevocationResource tested = createRevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("wrong", CLIENT_ID, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
	}
	
	@Test
	public void operationFailsOnMissingTokenParam() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createAccessToken(tokensManagement);
		RevocationResource tested = createRevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke(null, CLIENT_ID, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_BAD_REQUEST));
		assertThat(response.readEntity(String.class), containsString("invalid_request"));
	}
	
	@Test
	public void revokedAccessTokenIsNotListed() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createAccessToken(tokensManagement);
		RevocationResource tested = createRevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		setupInvocationContext(CLIENT_ENTITY_ID);
		
		Response response = tested.revoke("ac", CLIENT_ID, RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
		assertThat(tokensManagement.getAllTokens(INTERNAL_ACCESS_TOKEN).size(), is(0));
	}

	@Test
	public void revokedRefreshTokenIsNotListed() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createToken(tokensManagement, OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN, "x");
		RevocationResource tested = createRevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		setupInvocationContext(CLIENT_ENTITY_ID);
		
		
		Response response = tested.revoke("ref", CLIENT_ID, RevocationResource.TOKEN_TYPE_REFRESH, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));

		assertThat(tokensManagement.getAllTokens(OAuthRefreshTokenRepository.INTERNAL_REFRESH_TOKEN).size(), is(0));
	}

	
	@Test
	public void logoutIsNotWorkingWithoutScope() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = mock(SessionManagement.class);
		LoginSession session = mock(LoginSession.class);
		when(session.getId()).thenReturn("111");
		when(sessionManagement.getOwnedSession(new EntityParam(123l), "realm")).thenReturn(session);
		createAccessToken(tokensManagement);
		AuthenticationRealm realm = mock(AuthenticationRealm.class);
		when(realm.getName()).thenReturn("realm");
		setupInvocationContext(CLIENT_ENTITY_ID);
		RevocationResource tested = createRevocationResource(tokensManagement, sessionManagement, 
				realm);
		
		Response response = tested.revoke("ac", CLIENT_ID, RevocationResource.TOKEN_TYPE_ACCESS, "true");
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_BAD_REQUEST));
		assertThat(response.readEntity(String.class), containsString("invalid_scope"));
		verifyZeroInteractions(sessionManagement);
	}

	@Test
	public void logoutIsWorkingWithScope() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = mock(SessionManagement.class);
		LoginSession session = mock(LoginSession.class);
		when(session.getId()).thenReturn("111");
		when(sessionManagement.getOwnedSession(new EntityParam(123l), "realm")).thenReturn(session);
		createAccessToken(tokensManagement, RevocationResource.LOGOUT_SCOPE);
		AuthenticationRealm realm = mock(AuthenticationRealm.class);
		when(realm.getName()).thenReturn("realm");
		setupInvocationContext(CLIENT_ENTITY_ID);
		RevocationResource tested = createRevocationResource(tokensManagement, sessionManagement, 
				realm);
		
		Response response = tested.revoke("ac", CLIENT_ID, RevocationResource.TOKEN_TYPE_ACCESS, "true");

		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
		verify(sessionManagement).removeSession("111", true);
	}
	
	private void createAccessToken(TokensManagement tokensManagement, String... scopes) throws Exception
	{
		createToken(tokensManagement, INTERNAL_ACCESS_TOKEN, scopes);
	}
	
	private void createToken(TokensManagement tokensManagement, String type, String... scopes) throws Exception
	{
		OAuthToken token = new OAuthToken();
		token.setAccessToken("ac");
		token.setRefreshToken("ref");
		token.setClientUsername(CLIENT_ID);
		token.setClientId(CLIENT_ENTITY_ID);
		if (scopes.length > 0)
			token.setEffectiveScope(scopes);
		tokensManagement.addToken(type, type.equals(INTERNAL_ACCESS_TOKEN) ? token.getAccessToken() : token.getRefreshToken(), 
				new EntityParam(CLIENT_ENTITY_ID), token.getSerialized(), new Date(), new Date());
		
	}
	
	private RevocationResource createRevocationResource(TokensManagement tokensManagement, 
			SessionManagement sessionManagement, 
			AuthenticationRealm realm)
	{
		OAuthRefreshTokenRepository refreshTokenRepository = new OAuthRefreshTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		OAuthAccessTokenRepository accessTokenRepository = new OAuthAccessTokenRepository(tokensManagement, 
				mock(SecuredTokensManagement.class));
		
		return new RevocationResource(accessTokenRepository, refreshTokenRepository, sessionManagement, realm, false);
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
}
