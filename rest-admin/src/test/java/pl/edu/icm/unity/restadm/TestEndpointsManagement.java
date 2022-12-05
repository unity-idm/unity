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

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

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
		List<ResolvedEndpoint> returnedL = m.readValue(contents,
				new TypeReference<List<ResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(1));
		ResolvedEndpoint returned = returnedL.get(0);
		assertThat(returned.getEndpoint().getConfiguration().getAuthenticationOptions(),
			is(Lists.newArrayList(AUTHENTICATION_FLOW_PASS)));
		assertThat(returned.getEndpoint().getContextAddress(), is("/restadm"));
		assertThat(returned.getEndpoint().getConfiguration().getDescription(), is("desc"));
		assertThat(returned.getEndpoint().getConfiguration().getDisplayedName(), 
				is(new I18nString("restAdmin")));
		assertThat(returned.getName(), is("restAdmin"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpoint.NAME));
	}

	@Test
	public void informationOnDeployedEndpointIsReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();

		String contents = client.execute(host, deploy, getClientContext(host), new BasicHttpClientResponseHandler());
		ResolvedEndpoint returned = m.readValue(contents, ResolvedEndpoint.class);

		assertThat(returned.getEndpoint().getConfiguration().getAuthenticationOptions(), is(
				Lists.newArrayList("ApassRESTFlow")));
		assertThat(returned.getEndpoint().getContextAddress(), is("/contextA"));
		assertThat(returned.getEndpoint().getConfiguration().getDescription(), is("desc"));
		assertThat(returned.getEndpoint().getConfiguration().getDisplayedName(), 
				is(new I18nString("endpoint")));
		assertThat(returned.getName(), is("newEndpoint"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpoint.NAME));
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
		List<ResolvedEndpoint> returnedL = m.readValue(contents,
				new TypeReference<List<ResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(1));
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
		List<ResolvedEndpoint> returnedL = m.readValue(contents3,
				new TypeReference<List<ResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(2));

		ResolvedEndpoint returned = getEndpointById(returnedL, "newEndpoint");
		
		assertThat(returned.getEndpoint().getConfiguration().getAuthenticationOptions(), is(
				Lists.newArrayList("ApassRESTFlow")));
		assertThat(returned.getEndpoint().getContextAddress(), is("/contextA"));
		assertThat(returned.getEndpoint().getConfiguration().getDescription(), is("desc2"));
		assertThat(returned.getEndpoint().getConfiguration().getDisplayedName(), 
				is(new I18nString("endpoint2")));
		assertThat(returned.getName(), is("newEndpoint"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpoint.NAME));
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
		List<ResolvedEndpoint> returnedL = m.readValue(contents3,
				new TypeReference<List<ResolvedEndpoint>>() {});

		assertThat(returnedL.size(), is(2));

		ResolvedEndpoint returned = getEndpointById(returnedL, "newEndpoint");
		assertThat(returned.getEndpoint().getConfiguration().getAuthenticationOptions(), is(
				Lists.newArrayList("ApassRESTFlow")));
		assertThat(returned.getEndpoint().getContextAddress(), is("/contextA"));
		assertThat(returned.getEndpoint().getConfiguration().getDescription(), is("desc"));
		assertThat(returned.getEndpoint().getConfiguration().getDisplayedName(), 
				is(new I18nString("endpoint")));
		assertThat(returned.getName(), is("newEndpoint"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpoint.NAME));
	}

	private ResolvedEndpoint getEndpointById(List<ResolvedEndpoint> returnedL, String id)
	{
		for (ResolvedEndpoint e: returnedL)
			if (e.getName().equals(id))
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
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("endpoint"),
				"desc",
				authn,
				"",
				"testr");
		String jsonconfig = m.writeValueAsString(config);
		System.out.println(jsonconfig);
		deploy.setEntity(new StringEntity(jsonconfig, ContentType.APPLICATION_JSON));
		return deploy;
	}

	private HttpPut getUpdateRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPut update = new HttpPut("/restadm/v1/endpoint/newEndpoint");
		List<String> authn = Lists.newArrayList(AUTHENTICATION_FLOW_PASS);
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("endpoint2"),
				"desc2",
				authn,
				"",
				"testr");
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
