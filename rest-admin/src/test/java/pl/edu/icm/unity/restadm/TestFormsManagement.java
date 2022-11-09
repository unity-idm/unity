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
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import eu.unicore.util.httpclient.HttpResponseHandler;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;

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
		ClassicHttpResponse response = client.execute(host, add, localcontext, HttpResponseHandler.INSTANCE);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());

		HttpGet get = new HttpGet("/restadm/v1/registrationForms");
		ClassicHttpResponse responseGet = client.execute(host, get, localcontext, HttpResponseHandler.INSTANCE);
		String contents = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contents);
		assertEquals(contents, Status.OK.getStatusCode(), responseGet.getCode());
		List<RegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RegistrationForm>>() {});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0), is(getRegistrationForm()));
	}

	@Test
	public void removedFormIsNotReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		ClassicHttpResponse response = client.execute(host, add, localcontext, HttpResponseHandler.INSTANCE);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		
		HttpDelete delete = new HttpDelete("/restadm/v1/registrationForm/exForm");
		ClassicHttpResponse deleteResponse =client.execute(host, delete, localcontext, HttpResponseHandler.INSTANCE);
		assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResponse.getCode());
		
		HttpGet get = new HttpGet("/restadm/v1/registrationForms");
		ClassicHttpResponse responseGet = client.execute(host, get, localcontext, HttpResponseHandler.INSTANCE);

		String contents = EntityUtils.toString(responseGet.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), responseGet.getCode());
		List<RegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RegistrationForm>>() {});
		assertThat(returnedL.isEmpty(), is(true));
	}

	@Test
	public void updatedFormIsReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		ClassicHttpResponse response = client.execute(host, add, localcontext, HttpResponseHandler.INSTANCE);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());

		HttpPut update = getUpdateRequest();
		ClassicHttpResponse response2 = client.execute(host, update, localcontext, HttpResponseHandler.INSTANCE);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getCode());

		HttpGet get = new HttpGet("/restadm/v1/registrationForms");
		ClassicHttpResponse getResponse = client.execute(host, get, localcontext, HttpResponseHandler.INSTANCE);

		String contents = EntityUtils.toString(getResponse.getEntity());
		System.out.println(contents);
		assertEquals(contents, Status.OK.getStatusCode(), getResponse.getCode());
		List<RegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RegistrationForm>>() {});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0), is(getUpdatedRegistrationForm()));
	}

	private RegistrationForm getRegistrationForm()
	{
		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setIdentityType(UsernameIdentity.ID);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		return new RegistrationFormBuilder()
			.withName("exForm")
			.withAddedIdentityParam(idParam)
			.withPubliclyAvailable(true)
			.withCollectComments(true)
			.withDefaultCredentialRequirement(CRED_REQ_PASS)
			.build();
	}

	private RegistrationForm getUpdatedRegistrationForm()
	{
		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setIdentityType(X500Identity.ID);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		return new RegistrationFormBuilder()
			.withName("exForm")
			.withAddedIdentityParam(idParam)
			.withPubliclyAvailable(false)
			.withCollectComments(true)
			.withDefaultCredentialRequirement(CRED_REQ_PASS)
			.build();
	}
	
	private void configureRequest(HttpUriRequestBase request, RegistrationForm form)
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
