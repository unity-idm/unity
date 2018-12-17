/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.Mockito;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.token.RevocationResource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

public class RevocationResourceTest
{	
	@Test
	public void userIdMustMatchTokenOwner() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createAccessToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
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
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("wrong", "clientId", RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
	}
	
	@Test
	public void operationFailsOnMissingTokenParam() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createAccessToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke(null, "clientId", RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_BAD_REQUEST));
		assertThat(response.readEntity(String.class), containsString("invalid_request"));
	}
	
	@Test
	public void revokedAccessTokenIsNotListed() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createAccessToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("ac", "clientId", RevocationResource.TOKEN_TYPE_ACCESS, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));

		assertThat(tokensManagement.getAllTokens(OAuthProcessor.INTERNAL_ACCESS_TOKEN).size(), is(0));
	}

	@Test
	public void revokedRefreshTokenIsNotListed() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createToken(tokensManagement, OAuthProcessor.INTERNAL_REFRESH_TOKEN, "x");
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("ref", "clientId", RevocationResource.TOKEN_TYPE_REFRESH, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));

		assertThat(tokensManagement.getAllTokens(OAuthProcessor.INTERNAL_REFRESH_TOKEN).size(), is(0));
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
		
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				realm);
		
		Response response = tested.revoke("ac", "clientId", RevocationResource.TOKEN_TYPE_ACCESS, "true");
		
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
		
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				realm);
		
		Response response = tested.revoke("ac", "clientId", RevocationResource.TOKEN_TYPE_ACCESS, "true");
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
		verify(sessionManagement).removeSession("111", true);
	}
	
	private void createAccessToken(TokensManagement tokensManagement, String... scopes) throws Exception
	{
		createToken(tokensManagement, OAuthProcessor.INTERNAL_ACCESS_TOKEN, scopes);
	}
	
	private void createToken(TokensManagement tokensManagement, String type, String... scopes) throws Exception
	{
		OAuthToken token = new OAuthToken();
		token.setAccessToken("ac");
		token.setRefreshToken("ref");
		token.setClientUsername("clientId");
		if (scopes.length > 0)
			token.setEffectiveScope(scopes);
		tokensManagement.addToken(type, type.equals(OAuthProcessor.INTERNAL_ACCESS_TOKEN) ? token.getAccessToken() : token.getRefreshToken(), 
				new EntityParam(123l), token.getSerialized(), new Date(), new Date());
		
	}
	
}
