/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;

import pl.edu.icm.unity.engine.builders.RegistrationRequestBuilder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeParamRepresentation;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RESTRegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

public class TestRegRequests extends RESTAdminTestBase
{
	@Autowired
	private RegistrationsManagement regMan;
	
	@Before
	public void init() throws EngineException
	{
		attrsMan.addAttributeType(new AttributeType("attr", new StringAttributeSyntax()));
		groupsMan.addGroup(new Group("/A"));
		regMan.addForm(getRegistrationForm());
	}
	
	
	@Test
	public void allRequestsAreReturned() throws Exception
	{
		RegistrationRequest request = getRequest();
		RegistrationContext context = new RegistrationContext(true, false, TriggeringMode.manualAtLogin);
		regMan.submitRegistrationRequest(request, context);
		regMan.submitRegistrationRequest(request, context);
		
		HttpGet get = new HttpGet("/restadm/v1/registrationRequests");
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contents = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contents);
		assertEquals(contents, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());

		List<RESTRegistrationRequestState> returnedL = m.readValue(contents, 
				new TypeReference<List<RESTRegistrationRequestState>>() {});
		assertThat(returnedL.size(), is(2));
		assertEqual(request, returnedL.get(0));
		assertEqual(request, returnedL.get(1));
	}
	
	@Test
	public void requestsIsReturnedById() throws Exception
	{
		RegistrationRequest request = getRequest();
		RegistrationContext context = new RegistrationContext(true, false, TriggeringMode.manualAtLogin);
		String id = regMan.submitRegistrationRequest(request, context);
		
		HttpGet get = new HttpGet("/restadm/v1/registrationRequest/" + id);
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contents = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contents);
		assertEquals(contents, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());

		RESTRegistrationRequestState returned = m.readValue(contents, RESTRegistrationRequestState.class);
		assertEqual(request, returned);
	}

	private void assertEqual(RegistrationRequest expected, RESTRegistrationRequestState returned)
	{
		assertThat(returned.getRequest().getAttributes().size(), is(expected.getAttributes().size()));
		assertThat(returned.getRequest().getAttributes().get(0), 
				is(new AttributeParamRepresentation(expected.getAttributes().get(0))));
		assertThat(returned.getRequest().getIdentities(), is(expected.getIdentities()));
	}
	
	private RegistrationRequest getRequest()
	{
		return new RegistrationRequestBuilder().
				withFormId("exForm").
				withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "user")).
				withAddedAttribute(new StringAttribute("attr", "/A", AttributeVisibility.local, "val")).
				build();
	}
	
	private RegistrationForm getRegistrationForm()
	{
		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setIdentityType(UsernameIdentity.ID);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		AttributeRegistrationParam aParam = new AttributeRegistrationParam();
		aParam.setAttributeType("attr");
		aParam.setGroup("/A");
		return new RegistrationFormBuilder()
			.withName("exForm")
			.withAddedIdentityParam(idParam)
			.withAddedAttributeParam(aParam)
			.withPubliclyAvailable(true)
			.withDefaultCredentialRequirement(CRED_REQ_PASS)
			.build();
	}
}
