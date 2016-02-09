/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

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
		HttpResponse response = client.execute(host, add, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());

		HttpGet get = new HttpGet("/restadm/v1/registrationForms");
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contents = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contents);
		assertEquals(contents, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());
		List<RegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RegistrationForm>>() {});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0), is(getRegistrationForm()));
	}

	@Test
	public void removedFormIsNotReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		HttpResponse response = client.execute(host, add, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
		
		HttpDelete delete = new HttpDelete("/restadm/v1/registrationForm/exForm");
		HttpResponse deleteResponse =client.execute(host, delete, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatusLine().getStatusCode());
		
		HttpGet get = new HttpGet("/restadm/v1/registrationForms");
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contents = EntityUtils.toString(responseGet.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());
		List<RegistrationForm> returnedL = m.readValue(contents, 
				new TypeReference<List<RegistrationForm>>() {});
		assertThat(returnedL.isEmpty(), is(true));
	}

	@Test
	public void updatedFormIsReturned() throws Exception
	{
		HttpPost add = getAddRequest();
		HttpResponse response = client.execute(host, add, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());

		HttpPut update = getUpdateRequest();
		HttpResponse response2 = client.execute(host, update, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getStatusLine().getStatusCode());

		HttpGet get = new HttpGet("/restadm/v1/registrationForms");
		HttpResponse getResponse = client.execute(host, get, localcontext);

		String contents = EntityUtils.toString(getResponse.getEntity());
		System.out.println(contents);
		assertEquals(contents, Status.OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
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
	
	private void configureRequest(HttpEntityEnclosingRequestBase request, RegistrationForm form)
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
