/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
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
		Identity id1 = createUsernameUser("u1", InternalAuthorizationManagerImpl.USER_ROLE,
				DEF_PASSWORD, CRED_REQ_PASS);
		Identity id2 = createUsernameUser("u2", InternalAuthorizationManagerImpl.USER_ROLE,
				DEF_PASSWORD, CRED_REQ_PASS);
		createUsernameUser("admin1", InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE,
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
		List<JsonNode> tokens = getTokensFromRESTAPI(localcontext, "type1");	
		
		assertThat(tokens.size(), is(3));
		assertThat(tokens.get(0).get("type").asText(), is("type1"));
	}
	
	@Test
	public void shouldReturnAllToken() throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/tokens");
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		
		assertThat(responseGet.getStatusLine().getStatusCode(), is(Status.OK.getStatusCode()));
		List<JsonNode> returned = m.readValue(contentsGet,
				new TypeReference<List<JsonNode>>()
				{
				});
		
		Set<String> types = returned.stream().map(s -> s.get("type").asText()).collect(Collectors.toSet());
		assertThat(types, hasItem("type1"));
		assertThat(types, hasItem("type2"));
	}
	
	@Test
	public void shouldReturnOnlyOwnedTokenWithType() throws Exception
	{
		HttpContext u1 = getClientContext(host, "u1", DEF_PASSWORD);
		List<JsonNode> tokens = getTokensFromRESTAPI(u1, "type1");
		assertThat(tokens.size(), is(1));
		HttpContext u2 = getClientContext(host, "u2", DEF_PASSWORD);
		tokens = getTokensFromRESTAPI(u2, "type1");
		assertThat(tokens.size(), is(2));
		tokens = getTokensFromRESTAPI(u2, "type2");
		assertThat(tokens.size(), is(1));
	}
	
	@Test
	public void shouldRemoveToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		HttpContext u2 = getClientContext(host, "u2", DEF_PASSWORD);
		HttpResponse responseDel = client.execute(host, del, u2);

		assertThat(responseDel.getStatusLine().getStatusCode(), is(Status.NO_CONTENT.getStatusCode()));
		List<JsonNode> tokens = getTokensFromRESTAPI(u2, "type2");
		assertThat(tokens.size(), is(0));
	}

	@Test
	public void shouldDeniedRemoveNotOwnedToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		HttpContext u1 = getClientContext(host, "u1", DEF_PASSWORD);
		HttpResponse responseDel = client.execute(host, del, u1);

		assertThat(responseDel.getStatusLine().getStatusCode(), is(Status.BAD_REQUEST.getStatusCode()));
	}
	
	@Test
	public void shouldReturnErrorWhenRemoveMissingToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v5");
		HttpContext u1 = getClientContext(host, "u1", DEF_PASSWORD);
		HttpResponse responseDel = client.execute(host, del, u1);
		
		assertThat(responseDel.getStatusLine().getStatusCode(), is(Status.BAD_REQUEST.getStatusCode()));
	}

	private List<JsonNode> getTokensFromRESTAPI(HttpContext context, String type) throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/tokens?type=" + type);
		HttpResponse responseGet = client.execute(host, get, context);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		assertThat(responseGet.getStatusLine().getStatusCode(), is( Status.OK.getStatusCode()));
		
		List<JsonNode> returned = m.readValue(contentsGet,
				new TypeReference<List<JsonNode>>()
				{
				});
		
		return returned;
	}

}
