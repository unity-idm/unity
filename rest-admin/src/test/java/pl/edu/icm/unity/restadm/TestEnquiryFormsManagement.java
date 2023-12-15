/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestEnquiryFormNotifications;
import io.imunity.rest.mappers.registration.layout.FormLayoutSettingsMapper;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

/**
 * Registration forms management test
 * @author Krzysztof Benedyczak
 */
public class TestEnquiryFormsManagement extends RESTAdminTestBase
{
	@Test
	public void addedFormIsReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		HttpGet get = new HttpGet("/restadm/v1/enquiryForms");
		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());

		List<RestEnquiryForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RestEnquiryForm>>() {});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0), is(getEnquiryForm()));
	}

	@Test
	public void removedFormIsNotReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		HttpDelete delete = new HttpDelete("/restadm/v1/enquiryForm/exForm");
		try(ClassicHttpResponse deleteResponse =client.executeOpen(host, delete, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResponse.getCode());
		}
		HttpGet get = new HttpGet("/restadm/v1/enquiryForms");

		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		List<RestEnquiryForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RestEnquiryForm>>() {});
		assertThat(returnedL.isEmpty(), is(true));
	}

	@Test
	public void updatedFormIsReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		HttpPut update = getUpdateRequest();
		try(ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		HttpGet get = new HttpGet("/restadm/v1/enquiryForms");

		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());

		List<RestEnquiryForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RestEnquiryForm>>() {});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0), is(getUpdatedEnquiryForm()));
	}

	private RestEnquiryForm getEnquiryForm()
	{
		return  RestEnquiryForm.builder()
			.withName("exForm")
			.withType(EnquiryType.STICKY.name())
			.withTargetGroups(List.of("/"))
			.withTargetCondition("true")
			.withCollectComments(true)
			.withFormInformation(RestI18nString.builder().build())
			.withLayoutSettings(FormLayoutSettingsMapper.map(FormLayoutSettings.DEFAULT))
			.withNotificationsConfiguration(RestEnquiryFormNotifications.builder().build())
			.build();
	}

	private RestEnquiryForm getUpdatedEnquiryForm()
	{
		return RestEnquiryForm.builder()
			.withName("exForm")
			.withType(EnquiryType.STICKY.name())
			.withTargetGroups(List.of("/"))
			.withTargetCondition("true")
			.withCollectComments(true)
			.withFormInformation(RestI18nString.builder().build())
			.withLayoutSettings(FormLayoutSettingsMapper.map(FormLayoutSettings.DEFAULT))
			.withNotificationsConfiguration(RestEnquiryFormNotifications.builder().build())
			.build();
	}
	

	private void configureRequest(HttpUriRequestBase request, RestEnquiryForm form)
			throws UnsupportedEncodingException, JsonProcessingException
	{
		String jsonform = m.writeValueAsString(form);
		request.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
	}
	
	private HttpPost getAddRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPost addForm = new HttpPost("/restadm/v1/enquiryForm");
		configureRequest(addForm, getEnquiryForm());
		return addForm;
	}

	private HttpPut getUpdateRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPut update = new HttpPut("/restadm/v1/enquiryForm");
		configureRequest(update, getUpdatedEnquiryForm());
		return update;
	}
}
