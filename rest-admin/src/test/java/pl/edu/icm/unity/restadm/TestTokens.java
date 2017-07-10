/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestTokens extends RESTAdminTestBase
{

	@Autowired
	TokensManagement tokenMan;

	@Before
	public void addTokens() throws Exception
	{
		Identity id1 = createUsernameUser("u1", AuthorizationManagerImpl.USER_ROLE,
				DEF_PASSWORD, CRED_REQ_PASS);
		Identity id2 = createUsernameUser("u2", AuthorizationManagerImpl.USER_ROLE,
				DEF_PASSWORD, CRED_REQ_PASS);
		createUsernameUser("admin1", AuthorizationManagerImpl.SYSTEM_MANAGER_ROLE,
				DEF_PASSWORD, CRED_REQ_PASS);

		Date now = new Date();
		Date exp = new Date(now.getTime() + 3600);
		tokenMan.addToken("type1", "v1", new EntityParam(id1), "v1".getBytes(), now, exp);
		tokenMan.addToken("type1", "v2", new EntityParam(id2), "v2".getBytes(), now, exp);
		tokenMan.addToken("type1", "v3", new EntityParam(id2), "v3".getBytes(), now, exp);
		tokenMan.addToken("type2", "v4", new EntityParam(id2), "v4".getBytes(), now, exp);
	}

	@Test
	public void shouldReturnAllTokenWithType() throws Exception
	{
		List<Token> tokens = getTokensFromRESTAPI(localcontext, "type1");	
		assertThat(tokens.size(), is(3));
		assertEquals("type1", tokens.get(0).getType());
		
		
	}
	
	@Test
	public void shouldReturnAllToken() throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/tokens");
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		assertEquals(contentsGet, Status.OK.getStatusCode(),
				responseGet.getStatusLine().getStatusCode());
		List<Token> returned = m.readValue(contentsGet,
				new TypeReference<List<Token>>()
				{
				});
		
		Set<String> types = returned.stream().map(s -> s.getType()).collect(Collectors.toSet());
		assertTrue(types.contains("type1"));
		assertTrue(types.contains("type2"));
	}
	
	@Test
	public void shouldReturnOnlyOwnedTokenWithType() throws Exception
	{
		HttpContext u1 = getClientContext(client, host, "u1", DEF_PASSWORD);
		List<Token> tokens = getTokensFromRESTAPI(u1, "type1");
		assertThat(tokens.size(), is(1));
		HttpContext u2 = getClientContext(client, host, "u2", DEF_PASSWORD);
		tokens = getTokensFromRESTAPI(u2, "type1");
		assertThat(tokens.size(), is(2));
		tokens = getTokensFromRESTAPI(u2, "type2");
		assertThat(tokens.size(), is(1));
	}
	
	@Test
	public void shouldRemoveToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		HttpContext u2 = getClientContext(client, host, "u2", DEF_PASSWORD);
		HttpResponse responseDel = client.execute(host, del, u2);

		assertEquals(Status.NO_CONTENT.getStatusCode(),
				responseDel.getStatusLine().getStatusCode());
		List<Token> tokens = getTokensFromRESTAPI(u2, "type2");
		assertThat(tokens.size(), is(0));
	}

	@Test
	public void shouldDeniedRemoveNotOwnedToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		HttpContext u1 = getClientContext(client, host, "u1", DEF_PASSWORD);
		HttpResponse responseDel = client.execute(host, del, u1);
		assertEquals(Status.FORBIDDEN.getStatusCode(),
				responseDel.getStatusLine().getStatusCode());
	}

	private List<Token> getTokensFromRESTAPI(HttpContext context, String type) throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/tokens?type=" + type);
		HttpResponse responseGet = client.execute(host, get, context);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		assertEquals(contentsGet, Status.OK.getStatusCode(),
				responseGet.getStatusLine().getStatusCode());
		List<Token> returned = m.readValue(contentsGet,
				new TypeReference<List<Token>>()
				{
				});
		
		return returned;
	}

}
