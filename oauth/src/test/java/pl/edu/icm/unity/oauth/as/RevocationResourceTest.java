/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.oauth.as;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import javax.ws.rs.core.Response;

import org.junit.Test;

import pl.edu.icm.unity.oauth.as.token.RevocationResource;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.types.basic.EntityParam;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;

public class RevocationResourceTest
{
	@Test
	public void userIdMustMatchTokenOwner() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement);
		
		Response response = tested.revoke("ac", "clientIdOther", null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_UNAUTHORIZED));
		assertThat(response.readEntity(String.class), containsString("invalid_client"));
	}
	
	@Test
	public void nonExistingTokenIsAccepted() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement);
		
		Response response = tested.revoke("wrong", "clientId", null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));
	}
	
	@Test
	public void operationFailsOnMissingTokenParam() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement);
		
		Response response = tested.revoke(null, "clientId", null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_BAD_REQUEST));
		assertThat(response.readEntity(String.class), containsString("invalid_request"));
	}
	
	@Test
	public void validTokenIsRevoked() throws Exception
	{
		TokensManagement tokensManagement = new MockTokensMan();
		createToken(tokensManagement);
		RevocationResource tested = new RevocationResource(tokensManagement);
		
		Response response = tested.revoke("ac", "clientId", null);
		
		assertThat(response.getStatus(), is(HTTPResponse.SC_OK));
		assertThat(response.hasEntity(), is(false));

		assertThat(tokensManagement.getAllTokens(OAuthProcessor.INTERNAL_ACCESS_TOKEN).size(), is(0));
	}
	
	private void createToken(TokensManagement tokensManagement) throws Exception
	{
		OAuthToken token = new OAuthToken();
		token.setAccessToken("ac");
		token.setClientName("clientId");
		tokensManagement.addToken(OAuthProcessor.INTERNAL_ACCESS_TOKEN, token.getAccessToken(), 
				new EntityParam(123l), token.getSerialized(), new Date(), new Date());
	}
}
