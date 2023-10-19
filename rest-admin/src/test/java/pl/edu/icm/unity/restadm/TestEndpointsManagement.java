/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.UnsupportedEncodingException;
import java.util.List;

import jakarta.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.endpoint.RestEndpointConfiguration;
import io.imunity.rest.api.types.endpoint.RestResolvedEndpoint;

/**
 * Endpoints management test
 * @author Krzysztof Benedyczak
 */
public class TestEndpointsManagement extends RESTAdminTestBase
{
	@Test
	public void deployedEndpointsAreReturned() throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		System.out.println(contents);
		List<RestResolvedEndpoint> returnedL = m.readValue(contents,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL).hasSize(1);
		RestResolvedEndpoint returned = returnedL.get(0);
		assertThat(returned.endpoint.configuration.authenticationOptions).isEqualTo(Lists.newArrayList(AUTHENTICATION_FLOW_PASS));
		assertThat(returned.endpoint.contextAddress).isEqualTo("/restadm");
		assertThat(returned.endpoint.configuration.description).isEqualTo("desc");
		assertThat(returned.endpoint.configuration.displayedName) 
				.isEqualTo(RestI18nString.builder().withDefaultValue(("restAdmin")).build());
		assertThat(returned.endpoint.name).isEqualTo("restAdmin");
		assertThat(returned.realm.name).isEqualTo("testr");
		assertThat(returned.type.name).isEqualTo(RESTAdminEndpoint.NAME);
	}

	@Test
	public void informationOnDeployedEndpointIsReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();

		String contents = client.execute(host, deploy, getClientContext(host), new BasicHttpClientResponseHandler());
		RestResolvedEndpoint returned = m.readValue(contents, RestResolvedEndpoint.class);
		
		assertThat(returned.endpoint.configuration.authenticationOptions).isEqualTo(
				Lists.newArrayList("ApassRESTFlow"));
		assertThat(returned.endpoint.contextAddress).isEqualTo("/contextA");
		assertThat(returned.endpoint.configuration.description).isEqualTo("desc");
		assertThat(returned.endpoint.configuration.displayedName).isEqualTo(RestI18nString.builder().withDefaultValue(("endpoint")).build());
		assertThat(returned.endpoint.name).isEqualTo("newEndpoint");
		assertThat(returned.realm.name).isEqualTo("testr");
		assertThat(returned.type.name).isEqualTo(RESTAdminEndpoint.NAME);
	}

	@Test
	public void undeployedEndpointIsNotReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, getClientContext(host), new BasicHttpClientResponseHandler());

		HttpDelete delete = new HttpDelete("/restadm/v1/endpoint/newEndpoint");
		client.execute(host, delete, getClientContext(host), new BasicHttpClientResponseHandler());

		HttpGet get = new HttpGet("/restadm/v1/endpoints");

		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		List<RestResolvedEndpoint> returnedL = m.readValue(contents,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL.size()).isEqualTo(1);
	}

	@Test
	public void updatedEndpointIsReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, getClientContext(host), new BasicHttpClientResponseHandler());

		HttpPut update = getUpdateRequest();
		ClassicHttpResponse response2 = client.executeOpen(host, update, getClientContext(host));
		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getCode());
		EntityUtils.consumeQuietly(response2.getEntity());
		response2.close();
		HttpGet get = new HttpGet("/restadm/v1/endpoints");

		String contents3 = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		List<RestResolvedEndpoint> returnedL = m.readValue(contents3,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL.size()).isEqualTo(2);

		RestResolvedEndpoint returned = getEndpointById(returnedL, "newEndpoint");
		
		assertThat(returned.endpoint.configuration.authenticationOptions).isEqualTo(
				Lists.newArrayList("ApassRESTFlow"));
		assertThat(returned.endpoint.contextAddress).isEqualTo("/contextA");
		assertThat(returned.endpoint.configuration.description).isEqualTo("desc2");
		assertThat(returned.endpoint.configuration.displayedName).isEqualTo(RestI18nString.builder().withDefaultValue(("endpoint2")).build());
		assertThat(returned.endpoint.name).isEqualTo("newEndpoint");
		assertThat(returned.realm.name).isEqualTo("testr");
		assertThat(returned.type.name).isEqualTo(RESTAdminEndpoint.NAME);
	}


	@Test
	public void onlyTheSetEntriesAreUpdated() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, getClientContext(host), new BasicHttpClientResponseHandler());

		HttpPut update = getEmptyUpdateRequest();
		ClassicHttpResponse response2 = client.executeOpen(host, update, getClientContext(host));
		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getCode());
		response2.close();
		HttpGet get = new HttpGet("/restadm/v1/endpoints");

		String contents3 = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		List<RestResolvedEndpoint> returnedL = m.readValue(contents3,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL.size()).isEqualTo(2);

		RestResolvedEndpoint returned = getEndpointById(returnedL, "newEndpoint");
		assertThat(returned.endpoint.configuration.authenticationOptions).isEqualTo(
				Lists.newArrayList("ApassRESTFlow"));
		assertThat(returned.endpoint.contextAddress).isEqualTo("/contextA");
		assertThat(returned.endpoint.configuration.description).isEqualTo("desc");
		assertThat(returned.endpoint.configuration.displayedName).isEqualTo( 
				RestI18nString.builder().withDefaultValue(("endpoint")).build());
		assertThat(returned.endpoint.name).isEqualTo("newEndpoint");
		assertThat(returned.realm.name).isEqualTo("testr");
		assertThat(returned.type.name).isEqualTo(RESTAdminEndpoint.NAME);
	}

	private RestResolvedEndpoint getEndpointById(List<RestResolvedEndpoint> returnedL, String id)
	{
		for (RestResolvedEndpoint e: returnedL)
			if (e.endpoint.name.equals(id))
				return e;
		fail("No endpoint with a given id " + id);
		throw new IllegalStateException();
	}
	
	@Test
	public void deployWithInvalidConfigurationResultsInBadRequest() throws Exception
	{
		HttpPost deploy = getDeployRequestWithInvalidAuthn();
		try(ClassicHttpResponse response = client.executeOpen(host, deploy, getClientContext(host))){
			assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getCode());
		}
	}

	private HttpPost getDeployRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		return getDeployRequest(AUTHENTICATION_FLOW_PASS);
	}

	private HttpPost getDeployRequestWithInvalidAuthn() throws UnsupportedEncodingException, JsonProcessingException
	{
		return getDeployRequest("Invalid authn");
	}

	private HttpPost getDeployRequest(String authnFlow) throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPost deploy = new HttpPost("/restadm/v1/endpoint/newEndpoint?typeId=" + RESTAdminEndpoint.NAME
				+ "&address=/contextA");
		List<String> authn = Lists.newArrayList(authnFlow);
		RestEndpointConfiguration config = RestEndpointConfiguration.builder()
				.withDisplayedName(RestI18nString.builder().withDefaultValue("endpoint").build())
				.withDescription("desc")
				.withAuthenticationOptions(authn)
				.withRealm("testr")
				.withConfiguration("")
				.build();
		
		String jsonconfig = m.writeValueAsString(config);
		System.out.println(jsonconfig);
		deploy.setEntity(new StringEntity(jsonconfig, ContentType.APPLICATION_JSON));
		return deploy;
	}

	private HttpPut getUpdateRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPut update = new HttpPut("/restadm/v1/endpoint/newEndpoint");
		List<String> authn = Lists.newArrayList(AUTHENTICATION_FLOW_PASS);
		RestEndpointConfiguration config = RestEndpointConfiguration.builder()
				.withDisplayedName(RestI18nString.builder().withDefaultValue("endpoint2").build())
				.withDescription("desc2")
				.withAuthenticationOptions(authn)
				.withRealm("testr")
				.withConfiguration("")
				.build();
		
		update.setEntity(new StringEntity(m.writeValueAsString(config), ContentType.APPLICATION_JSON));
		return update;
	}

	private HttpPut getEmptyUpdateRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPut update = new HttpPut("/restadm/v1/endpoint/newEndpoint");
		update.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));
		return update;
	}
}
