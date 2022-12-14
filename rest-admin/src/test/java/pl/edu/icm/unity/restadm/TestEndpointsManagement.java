/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

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
		HttpResponse response = client.execute(host, get, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		System.out.println(contents);
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		List<RestResolvedEndpoint> returnedL = m.readValue(contents,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(1));
		RestResolvedEndpoint returned = returnedL.get(0);
		assertThat(returned.endpoint.configuration.authenticationOptions,
			is(Lists.newArrayList(AUTHENTICATION_FLOW_PASS)));
		assertThat(returned.endpoint.contextAddress, is("/restadm"));
		assertThat(returned.endpoint.configuration.description, is("desc"));
		assertThat(returned.endpoint.configuration.displayedName, 
				is(RestI18nString.builder().withDefaultValue(("restAdmin")).build()));
		assertThat(returned.endpoint.name, is("restAdmin"));
		assertThat(returned.realm.name, is("testr"));
		assertThat(returned.type.name, is(RESTAdminEndpoint.NAME));
	}

	@Test
	public void informationOnDeployedEndpointIsReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();

		HttpResponse response = client.execute(host, deploy, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		RestResolvedEndpoint returned = m.readValue(contents, RestResolvedEndpoint.class);

		assertThat(returned.endpoint.configuration.authenticationOptions, is(
				Lists.newArrayList("ApassRESTFlow")));
		assertThat(returned.endpoint.contextAddress, is("/contextA"));
		assertThat(returned.endpoint.configuration.description, is("desc"));
		assertThat(returned.endpoint.configuration.displayedName, 
				is(RestI18nString.builder().withDefaultValue(("endpoint")).build()));
		assertThat(returned.endpoint.name, is("newEndpoint"));
		assertThat(returned.realm.name, is("testr"));
		assertThat(returned.type.name, is(RESTAdminEndpoint.NAME));
	}

	@Test
	public void undeployedEndpointIsNotReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, localcontext);

		HttpDelete delete = new HttpDelete("/restadm/v1/endpoint/newEndpoint");
		client.execute(host, delete, localcontext);

		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		HttpResponse response = client.execute(host, get, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		List<RestResolvedEndpoint> returnedL = m.readValue(contents,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(1));
	}

	@Test
	public void updatedEndpointIsReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, localcontext);

		HttpPut update = getUpdateRequest();
		HttpResponse response2 = client.execute(host, update, localcontext);

		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getStatusLine().getStatusCode());

		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		HttpResponse response3 = client.execute(host, get, localcontext);
		String contents3 = EntityUtils.toString(response3.getEntity());
		assertEquals(contents3, Status.OK.getStatusCode(), response3.getStatusLine().getStatusCode());
		List<RestResolvedEndpoint> returnedL = m.readValue(contents3,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(2));

		RestResolvedEndpoint returned = getEndpointById(returnedL, "newEndpoint");
		
		assertThat(returned.endpoint.configuration.authenticationOptions, is(
				Lists.newArrayList("ApassRESTFlow")));
		assertThat(returned.endpoint.contextAddress, is("/contextA"));
		assertThat(returned.endpoint.configuration.description, is("desc2"));
		assertThat(returned.endpoint.configuration.displayedName, 
				is(RestI18nString.builder().withDefaultValue(("endpoint2")).build()));
		assertThat(returned.endpoint.name, is("newEndpoint"));
		assertThat(returned.realm.name, is("testr"));
		assertThat(returned.type.name, is(RESTAdminEndpoint.NAME));
	}


	@Test
	public void onlyTheSetEntriesAreUpdated() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, localcontext);

		HttpPut update = getEmptyUpdateRequest();
		HttpResponse response2 = client.execute(host, update, localcontext);

		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getStatusLine().getStatusCode());

		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		HttpResponse response3 = client.execute(host, get, localcontext);
		String contents3 = EntityUtils.toString(response3.getEntity());
		assertEquals(contents3, Status.OK.getStatusCode(), response3.getStatusLine().getStatusCode());
		List<RestResolvedEndpoint> returnedL = m.readValue(contents3,
				new TypeReference<List<RestResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(2));

		RestResolvedEndpoint returned = getEndpointById(returnedL, "newEndpoint");
		assertThat(returned.endpoint.configuration.authenticationOptions, is(
				Lists.newArrayList("ApassRESTFlow")));
		assertThat(returned.endpoint.contextAddress, is("/contextA"));
		assertThat(returned.endpoint.configuration.description, is("desc"));
		assertThat(returned.endpoint.configuration.displayedName, 
				is(RestI18nString.builder().withDefaultValue(("endpoint")).build()));
		assertThat(returned.endpoint.name, is("newEndpoint"));
		assertThat(returned.realm.name, is("testr"));
		assertThat(returned.type.name, is(RESTAdminEndpoint.NAME));
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
		HttpResponse response = client.execute(host, deploy, localcontext);
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
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
