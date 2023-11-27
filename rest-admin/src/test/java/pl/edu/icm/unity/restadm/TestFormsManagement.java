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
import io.imunity.rest.api.types.registration.RestIdentityRegistrationParam;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import pl.edu.icm.unity.rest.mappers.registration.layout.FormLayoutSettingsMapper;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings;

/**
 * Registration forms management test
 * @author Krzysztof Benedyczak
 */
public class TestFormsManagement extends RESTAdminTestBase
{
	@Test
	public void addedFormIsReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		HttpGet get = new HttpGet("/restadm/v1/registrationForms");
		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		System.out.println("Response:\n" + contents);

		List<RestRegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RestRegistrationForm>>() {});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0), is(getRegistrationForm()));
	}

	@Test
	public void removedFormIsNotReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}
		HttpDelete delete = new HttpDelete("/restadm/v1/registrationForm/exForm");
		try(ClassicHttpResponse deleteResponse =client.executeOpen(host, delete, getClientContext(host))){
			assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResponse.getCode());
		}
		HttpGet get = new HttpGet("/restadm/v1/registrationForms");

		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		List<RestRegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RestRegistrationForm>>() {});
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
		HttpGet get = new HttpGet("/restadm/v1/registrationForms");

		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		System.out.println(contents);

		List<RestRegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RestRegistrationForm>>() {});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0), is(getUpdatedRegistrationForm()));
	}

	private RestRegistrationForm getRegistrationForm()
	{
		RestIdentityRegistrationParam idParam = RestIdentityRegistrationParam.builder()
				.withIdentityType(UsernameIdentity.ID)
				.withConfirmationMode(ConfirmationMode.ON_SUBMIT.name())
				.withRetrievalSettings(ParameterRetrievalSettings.interactive.name())
				.build();
		return  RestRegistrationForm.builder()
			.withName("exForm")
			.withIdentityParams(List.of(idParam))
			.withPubliclyAvailable(true)
			.withCollectComments(true)
			.withDefaultCredentialRequirement(CRED_REQ_PASS)
			.withFormInformation(RestI18nString.builder().build())
			.withFormInformation2ndStage(RestI18nString.builder().build())
			.withLayoutSettings(FormLayoutSettingsMapper.map(FormLayoutSettings.DEFAULT))
			.build();
	}

	private RestRegistrationForm getUpdatedRegistrationForm()
	{
		RestIdentityRegistrationParam idParam = RestIdentityRegistrationParam.builder()
				.withIdentityType(X500Identity.ID)
				.withConfirmationMode(ConfirmationMode.ON_SUBMIT.name())
				.withRetrievalSettings(ParameterRetrievalSettings.interactive.name())
				.build();
		return RestRegistrationForm.builder()
			.withName("exForm")
			.withIdentityParams(List.of(idParam))
			.withPubliclyAvailable(false)
			.withCollectComments(true)
			.withDefaultCredentialRequirement(CRED_REQ_PASS)
			.withFormInformation(RestI18nString.builder().build())
			.withFormInformation2ndStage(RestI18nString.builder().build())
			.withLayoutSettings(FormLayoutSettingsMapper.map(FormLayoutSettings.DEFAULT))
			.build();
	}
	

	private void configureRequest(HttpUriRequestBase request, RestRegistrationForm form)
			throws UnsupportedEncodingException, JsonProcessingException
	{
		String jsonform = m.writeValueAsString(form);
		System.out.println("Request to be sent:\n" + jsonform);
		request.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
	}
	
	private HttpPost getAddRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPost addForm = new HttpPost("/restadm/v1/registrationForm");
		configureRequest(addForm, getRegistrationForm());
		return addForm;
	}

	private HttpPut getUpdateRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPut update = new HttpPut("/restadm/v1/registrationForm");
		configureRequest(update, getUpdatedRegistrationForm());
		return update;
	}
}
