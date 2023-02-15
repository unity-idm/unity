/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.forms.PrefilledSet;

/**
 * 
 * @author P.Piernik
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EnquiryResponseEditorControllerTest
{
	@Mock
	EntityManagement mockIdMan;

	@Mock
	AttributesManagement mockAttrMan;

	@Mock
	GroupsManagement mockGroupMan;
	
	@Mock
	EnquiryManagement mockEnquiryMan;
	
	@Test
	public void shouldGetCorrectPreffiledSet() throws Exception
	{
		Map<String, GroupMembership> userGroups = new HashMap<>();
		userGroups.put("/", null);
		userGroups.put("/A", null);
		when(mockIdMan.getGroups(any())).thenReturn(userGroups);
		when(mockGroupMan.getGroupsByWildcard(eq("/**")))
				.thenReturn(Arrays.asList(new Group("/"), new Group("/A"), new Group("/A/B")));

		when(mockAttrMan.getAttributes(any(), any(), any())).thenReturn(
				Arrays.asList(new AttributeExt(new Attribute(InitializerCommon.CN_ATTR, null, "/A", Arrays.asList("DEMO-CN")), false)));
		
		
		initContext();
		EnquiryResponseEditorControllerV8 controller = new EnquiryResponseEditorControllerV8(null, null, null, null,
				null, null, null, mockGroupMan, mockIdMan, mockAttrMan, null, null, null, null, null);

		
		PrefilledSet prefilledForSticky = controller.getPrefilledSetForSticky(getForm());
		
		assertThat(prefilledForSticky.groupSelections.keySet().size(), is(1));
		assertThat(prefilledForSticky.groupSelections.get(0).getEntry().getSelectedGroups().size(), is(2));
		assertThat(prefilledForSticky.groupSelections.get(0).getEntry().getSelectedGroups().get(0), is("/"));
		assertThat(prefilledForSticky.groupSelections.get(0).getEntry().getSelectedGroups().get(1), is("/A"));
		assertThat(prefilledForSticky.attributes.get(0).getEntry().getValues().get(0), is("DEMO-CN"));
	}
	
	@Test
	public void shouldForwardGetStickyFormsToCoreManager() throws EngineException
	{

		EnquiryResponseEditorControllerV8 controller = new EnquiryResponseEditorControllerV8(null, mockEnquiryMan,
				null, null, null, null, null, null, null, null, null, null, null, null, null);
		initContext();
		controller.isStickyFormApplicable("test");
		verify(mockEnquiryMan).getAvailableEnquires(any(), any());
	}

	@Test
	public void shouldVerifyAsApplicableSticky() throws EngineException
	{

		EnquiryResponseEditorControllerV8 controller = new EnquiryResponseEditorControllerV8(null, mockEnquiryMan,
				null, null, null, null, null, null, null, null, null, null, null, null, null);

		when(mockEnquiryMan.getAvailableEnquires(any(), any())).thenReturn(Arrays.asList(getForm()));
		initContext();
		assertThat(controller.isStickyFormApplicable("sticky"), is(true));
	}
	
	@Test
	public void shouldCheckIfRequestExistsOnlyForLoggedUser() throws EngineException
	{
		EnquiryResponseEditorControllerV8 controller = new EnquiryResponseEditorControllerV8(null, mockEnquiryMan, null,
				null, null, null, null, null, null, null, null, null, null, null, null);
		EnquiryResponse res = new EnquiryResponse();
		res.setFormId("form1");
		EnquiryResponseState state1 = new EnquiryResponseState();
		state1.setEntityId(1L);
		state1.setRequest(res);
		state1.setStatus(RegistrationRequestStatus.pending);
		EnquiryResponseState state2 = new EnquiryResponseState();
		state2.setEntityId(1L);
		state2.setRequest(res);
		state2.setStatus(RegistrationRequestStatus.pending);

		when(mockEnquiryMan.getEnquiryResponses()).thenReturn(Arrays.asList(state1, state2));

		assertThat(controller.checkIfRequestExists("form1", 1L), is(true));
		assertThat(controller.checkIfRequestExists("form1", 2L), is(false));
	}

	private EnquiryForm getForm()
	{
		return new EnquiryFormBuilder().withName("sticky")
				.withDescription("desc")
				.withTargetGroups(new String[] { "/" })
				.withType(EnquiryType.STICKY)
				.withAddedAttributeParam()
				.withAttributeType(InitializerCommon.CN_ATTR)
				.withGroup("/A").withOptional(true)
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.withShowGroups(true)
				.endAttributeParam()
				.withAddedGroupParam()
				.withGroupPath("/**")
				.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.build();
	}
	
	private void initContext()
	{
		InvocationContext ctx = new InvocationContext(null, null, Collections.emptyList());
		InvocationContext.setCurrent(ctx);
		LoginSession ls = new LoginSession("1", new Date(), new Date(System.currentTimeMillis() + 1000), 50, 1,
				"r1", null, null, null);
		ctx.setLoginSession(ls);
	}

}
