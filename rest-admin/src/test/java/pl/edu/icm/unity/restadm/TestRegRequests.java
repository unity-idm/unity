/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;

import io.imunity.rest.api.types.registration.RestRegistrationRequest;
import io.imunity.rest.api.types.registration.RestRegistrationRequestState;
import io.imunity.rest.mappers.registration.RegistrationRequestMapper;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class TestRegRequests extends RESTAdminTestBase
{
	@Autowired
	private RegistrationsManagement regMan;
	
	@BeforeEach
	public void init() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("attr", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		regMan.addForm(getRegistrationForm());
	}
	
	
	@Test
	public void allRequestsAreReturned() throws Exception
	{
		RegistrationRequest request = getRequest();
		RegistrationContext context = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		regMan.submitRegistrationRequest(request, context);
		regMan.submitRegistrationRequest(request, context);
		
		HttpGet get = new HttpGet("/restadm/v1/registrationRequests");
		String contents = executeQuery(get);
		System.out.println("Response:\n" + contents);

		List<RestRegistrationRequestState> returnedL = m.readValue(contents, 
				new TypeReference<List<RestRegistrationRequestState>>() {});
		assertThat(returnedL.size()).isEqualTo(2);
		assertEqual(RegistrationRequestMapper.map(request), returnedL.get(0));
		assertEqual(RegistrationRequestMapper.map(request), returnedL.get(1));
	}
	
	@Test
	public void requestsIsReturnedById() throws Exception
	{
		RegistrationRequest request = getRequest();
		RegistrationContext context = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		String id = regMan.submitRegistrationRequest(request, context);
		
		HttpGet get = new HttpGet("/restadm/v1/registrationRequest/" + id);
		String contents = executeQuery(get);
		System.out.println("Response:\n" + contents);

		RestRegistrationRequestState returned = m.readValue(contents, RestRegistrationRequestState.class);
		assertEqual(RegistrationRequestMapper.map(request), returned);
	}

	private void assertEqual(RestRegistrationRequest expected, RestRegistrationRequestState returned)
	{
		assertThat(returned.request.attributes.size()).isEqualTo(expected.attributes.size());
		assertThat(returned.request.attributes.get(0)).isEqualTo(expected.attributes.get(0));
		assertThat(returned.request.identities).isEqualTo(expected.identities);
	}
	
	private RegistrationRequest getRequest()
	{
		return new RegistrationRequestBuilder().
				withFormId("exForm").
				withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "user")).
				withAddedAttribute(new Attribute("attr", StringAttributeSyntax.ID, "/A", 
						Lists.newArrayList("val"))).
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
