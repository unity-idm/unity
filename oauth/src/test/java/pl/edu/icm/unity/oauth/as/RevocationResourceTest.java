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

import pl.edu.icm.unity.oauth.as.token.RevocationResource;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.EntityParam;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

public class RevocationResourceTest
{	
	@Test
	public void userIdMustMatchTokenOwner() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("ac", "clientIdOther", null, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_UNAUTHORIZED));
		assertThat(response.readEntity(String.class), containsString("invalid_client"));
	}
	
	@Test
	public void nonExistingTokenIsAccepted() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("wrong", "clientId", null, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
	}
	
	@Test
	public void operationFailsOnMissingTokenParam() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke(null, "clientId", null, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_BAD_REQUEST));
		assertThat(response.readEntity(String.class), containsString("invalid_request"));
	}
	
	@Test
	public void validTokenIsRevoked() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = Mockito.mock(SessionManagement.class);
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				new AuthenticationRealm());
		
		Response response = tested.revoke("ac", "clientId", null, null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));

		assertThat(tokensManagement.getAllTokens(OAuthProcessor.INTERNAL_ACCESS_TOKEN).size(), is(0));
	}

	@Test
	public void logoutIsNotWorkingWithoutScope() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		SessionManagement sessionManagement = mock(SessionManagement.class);
		LoginSession session = mock(LoginSession.class);
		when(session.getId()).thenReturn("111");
		when(sessionManagement.getOwnedSession(new EntityParam(123l), "realm")).thenReturn(session);
		createToken(tokensManagement);
		AuthenticationRealm realm = mock(AuthenticationRealm.class);
		when(realm.getName()).thenReturn("realm");
		
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				realm);
		
		Response response = tested.revoke("ac", "clientId", null, "true");
		
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
		createToken(tokensManagement, RevocationResource.LOGOUT_SCOPE);
		AuthenticationRealm realm = mock(AuthenticationRealm.class);
		when(realm.getName()).thenReturn("realm");
		
		RevocationResource tested = new RevocationResource(tokensManagement, sessionManagement, 
				realm);
		
		Response response = tested.revoke("ac", "clientId", null, "true");
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
		verify(sessionManagement).removeSession("111", true);
	}
	
	private void createToken(TokensManagement tokensManagement, String... scopes) throws Exception
	{
		OAuthToken token = new OAuthToken();
		token.setAccessToken("ac");
		token.setClientUsername("clientId");
		if (scopes.length > 0)
			token.setScope(scopes);
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, token.getAccessToken(), 
				new EntityParam(123l), token.getSerialized(), new Date(), new Date());
	}
}
