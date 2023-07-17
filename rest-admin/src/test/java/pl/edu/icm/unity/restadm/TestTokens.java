/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;

/**
 * 
 * @author P.Piernik
 *
 */
public class TestTokens extends RESTAdminTestBase
{

	@Autowired
	TokensManagement tokenMan;

	@BeforeEach
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
		
		tokenMan.addToken("session", "session", new EntityParam(id2), getSampleSessionToken().getTokenContents(), now,
				exp);
	}
	
	@Test
	public void shouldReturnAllTokenWithType() throws Exception
	{
		List<JsonNode> tokens = getTokensFromRESTAPI(getClientContext(host), "type1");
		
		assertThat(tokens).hasSize(3);
		assertThat(tokens.get(0).get("type").asText()).isEqualTo("type1");
	}
	
	@Test
	public void shouldReturnAllToken() throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/tokens");
		String contentsGet = executeQuery(get);
		List<JsonNode> returned = m.readValue(contentsGet,
				new TypeReference<List<JsonNode>>()
				{
				});
		
		Set<String> types = returned.stream().map(s -> s.get("type").asText()).collect(Collectors.toSet());
		assertThat(types).contains("type1");
		assertThat(types).contains("type2");
		assertThat(types).contains("session");
	}
	
	@Test
	public void shouldReturnTokenWithJsonContent() throws Exception
	{

		HttpGet get = new HttpGet("/restadm/v1/tokens");
		String contentsGet = executeQuery(get);
		List<JsonNode> returned = m.readValue(contentsGet, new TypeReference<List<JsonNode>>()
		{
		});

		JsonNode jsonToken = returned.stream()
				.filter(t -> t.get("type")
						.asText()
						.equals("session")
						&& t.get("value")
								.asText()
								.equals("session"))
				.findFirst()
				.get();

		assertThat(jsonToken.get("contents")
				.get("realm").asText()).isEqualTo("realmv");

	}
	
	private LoginSession getSampleSessionToken()
	{
		LoginSession loginSession = new LoginSession();
		loginSession.setRealm("realmv");
		loginSession.setLastUsed(new Date(1));
		return loginSession;
	}
	
	@Test
	public void shouldReturnOnlyOwnedTokenWithType() throws Exception
	{
		HttpClientContext u1 = getClientContext(host, "u1", DEF_PASSWORD);
		List<JsonNode> tokens = getTokensFromRESTAPI(u1, "type1");
		assertThat(tokens.size()).isEqualTo(1);
		HttpClientContext u2 = getClientContext(host, "u2", DEF_PASSWORD);
		tokens = getTokensFromRESTAPI(u2, "type1");
		assertThat(tokens.size()).isEqualTo(2);
		tokens = getTokensFromRESTAPI(u2, "type2");
		assertThat(tokens.size()).isEqualTo(1);
	}
	
	@Test
	public void shouldRemoveToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		try(ClassicHttpResponse response = client.executeOpen(host, del, getClientContext(host, "u2", DEF_PASSWORD))){
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}
		List<JsonNode> tokens = getTokensFromRESTAPI(getClientContext(host, "u2", DEF_PASSWORD), "type2");
		assertThat(tokens.size()).isEqualTo(0);
	}

	@Test
	public void shouldDeniedRemoveNotOwnedToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v4");
		HttpClientContext u1 = getClientContext(host, "u1", DEF_PASSWORD);
		try(ClassicHttpResponse response = client.executeOpen(host, del, u1)){
			assertThat(response.getCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
		}
	}
	
	@Test
	public void shouldReturnErrorWhenRemoveMissingToken() throws Exception
	{
		HttpDelete del = new HttpDelete("/restadm/v1/token/type2/v5");
		try(ClassicHttpResponse response = client.executeOpen(host, del, getClientContext(host))){
			assertThat(response.getCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
		}
	}

	private List<JsonNode> getTokensFromRESTAPI(HttpClientContext context, String type) throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/tokens?type=" + type);
		String contentsGet = executeQuery(get, context);

		List<JsonNode> returned = m.readValue(contentsGet,
				new TypeReference<List<JsonNode>>()
				{
				});
		
		return returned;
	}

}
