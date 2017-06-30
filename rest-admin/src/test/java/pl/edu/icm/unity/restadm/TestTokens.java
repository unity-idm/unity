/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

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
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

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
	public void getTokensByAdmin() throws Exception
	{
		checkListToken(localcontext, "type1", 3);
		
	}
	
	@Test
	public void getTokensByRegularUser() throws Exception
	{
		HttpContext u1 = getClientContext(client, host, "u1", DEF_PASSWORD);
		checkListToken(u1, "type1", 1);
		HttpContext u2 = getClientContext(client, host, "u2", DEF_PASSWORD);
		checkListToken(u2, "type1", 2);
		checkListToken(u2, "type2", 1);
	}
	

	@Test
	public void removeToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		HttpContext u2 = getClientContext(client, host, "u2", DEF_PASSWORD);
		HttpResponse responseDel = client.execute(host, del, u2);

		assertEquals(Status.NO_CONTENT.getStatusCode(),
				responseDel.getStatusLine().getStatusCode());
		checkListToken(u2, "type2", 0);
	}

	@Test
	public void removeTokenByNotOwner() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		HttpContext u1 = getClientContext(client, host, "u1", DEF_PASSWORD);
		HttpResponse responseDel = client.execute(host, del, u1);
		assertEquals(Status.FORBIDDEN.getStatusCode(),
				responseDel.getStatusLine().getStatusCode());
	}

	private void checkListToken(HttpContext context, String type, int exp) throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/tokens/" + type);
		HttpResponse responseGet = client.execute(host, get, context);

		String contentsGet = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contentsGet);
		assertEquals(contentsGet, Status.OK.getStatusCode(),
				responseGet.getStatusLine().getStatusCode());

		List<InvitationWithCode> returned = m.readValue(contentsGet,
				new TypeReference<List<Token>>()
				{
				});
		assertThat(returned.size(), is(exp));
	}

}
