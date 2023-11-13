/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;

import io.imunity.rest.api.types.registration.RestEnquiryResponse;
import io.imunity.rest.api.types.registration.RestEnquiryResponseState;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.restadm.mappers.registration.EnquiryResponseMapper;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;

public class TestEnquiryResponses extends RESTAdminTestBase
{
	@Autowired
	private EnquiryManagement enqMan;
	
	@Before
	public void init() throws EngineException
	{
		aTypeMan.addAttributeType(new AttributeType("attr", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		enqMan.addEnquiry(getEnquiryForm());
	}
	
	
	@Test
	public void allRequestsAreReturned() throws Exception
	{
		EnquiryResponse response = getResponse();
		RegistrationContext context = new RegistrationContext(false, TriggeringMode.manualAtLogin);

		enqMan.submitEnquiryResponse(response, context);
		enqMan.submitEnquiryResponse(response, context);

		
		HttpGet get = new HttpGet("/restadm/v1/enquiryResponses");
		String contents = executeQuery(get);
		System.out.println("Response:\n" + contents);

		List<RestEnquiryResponseState> returnedL = m.readValue(contents, 
				new TypeReference<List<RestEnquiryResponseState>>() {});
		assertThat(returnedL.size(), is(2));
		assertEqual(EnquiryResponseMapper.map(response), returnedL.get(0));
		assertEqual(EnquiryResponseMapper.map(response), returnedL.get(1));
	}
	
	@Test
	public void responseIsReturnedById() throws Exception
	{
		EnquiryResponse request = getResponse();
		RegistrationContext context = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		String id = enqMan.submitEnquiryResponse(request, context);
		
		HttpGet get = new HttpGet("/restadm/v1/enquiryResponse/" + id);
		String contents = executeQuery(get);
		System.out.println("Response:\n" + contents);

		RestEnquiryResponseState returned = m.readValue(contents, RestEnquiryResponseState.class);
		assertEqual(EnquiryResponseMapper.map(request), returned);
	}

	private void assertEqual(RestEnquiryResponse expected, RestEnquiryResponseState returned)
	{
		assertThat(returned.request.attributes.size(), is(expected.attributes.size()));
		assertThat(returned.request.attributes.get(0), 
				is(expected.attributes.get(0)));
	}
	
	private EnquiryResponse getResponse()
	{
		return new EnquiryResponseBuilder().
				withFormId("exForm").
				withAddedAttribute(new Attribute("attr", StringAttributeSyntax.ID, "/A", 
						Lists.newArrayList("val"))).
				build();
	}
	
	private EnquiryForm getEnquiryForm()
	{
		
		AttributeRegistrationParam aParam = new AttributeRegistrationParam();
		aParam.setAttributeType("attr");
		aParam.setGroup("/A");
		return new EnquiryFormBuilder()
			.withName("exForm")
			.withType(EnquiryType.REQUESTED_MANDATORY)
			.withTargetCondition("true")
			.withTargetGroups(new String[] { "/" })
			.withAddedAttributeParam(aParam)
			.build();
	}
}
