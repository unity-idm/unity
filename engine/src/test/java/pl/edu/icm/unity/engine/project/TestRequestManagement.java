/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.registration.RequestType;

/**
 * 
 * @author P.Piernik
 *
 */
@ExtendWith(MockitoExtension.class)
public class TestRequestManagement extends TestProjectBase
{
	@Mock
	PublicRegistrationURLSupport mockPublicRegistrationURLSupport;

	private ProjectRequestManagementImpl projectRequestMan;

	@BeforeEach
	public void initDelegatedGroupMan()
	{
		projectRequestMan = new ProjectRequestManagementImpl(mockAuthz, mockRegistrationMan, mockEnquiryMan,
				mockGroupMan, mockIdMan,
				new ProjectAttributeHelper(mockAttrMan, mockAttrHelper, mockAtHelper), mockAttrHelper,
				mockPublicRegistrationURLSupport);
	}

	@Test
	public void shouldForwardGetRequestToCoreManagers() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));

		RegistrationRequestState requestFull = new RegistrationRequestState();
		RegistrationRequest request = new RegistrationRequest();
		request.setFormId("regForm");
		requestFull.setRequest(request);
		requestFull.setStatus(RegistrationRequestStatus.pending);

		EnquiryResponseState responseFull = new EnquiryResponseState();
		EnquiryResponse response = new EnquiryResponse();
		response.setFormId("enqForm");
		responseFull.setRequest(response);
		responseFull.setStatus(RegistrationRequestStatus.pending);

		when(mockRegistrationMan.getRegistrationRequests()).thenReturn(Arrays.asList(requestFull));
		when(mockEnquiryMan.getEnquiryResponses()).thenReturn(Arrays.asList(responseFull));

		List<ProjectRequest> requests = projectRequestMan.getRequests("/project");

		assertThat(requests).hasSize(2);
	}

	@Test
	public void shouldForwardAcceptRegistrationRequestToCoreManager() throws EngineException
	{
		String id = "1";
		RegistrationRequestState state = new RegistrationRequestState();
		state.setRequestId(id);
		when(mockRegistrationMan.getRegistrationRequest(eq(id))).thenReturn(state);

		projectRequestMan.accept(new ProjectRequestParam("/project", id, RequestOperation.SignUp,
				RequestType.Registration));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockRegistrationMan).processRegistrationRequest(argument.capture(), any(),
				eq(RegistrationRequestAction.accept), any(), any());
		assertThat(argument.getValue()).isEqualTo(id);
	}

	@Test
	public void shouldForwardAcceptEnquiryResponsesToCoreManager() throws EngineException
	{
		String id = "1";
		EnquiryResponseState state = new EnquiryResponseState();
		state.setRequestId(id);
		when(mockEnquiryMan.getEnquiryResponse(eq(id))).thenReturn(state);

		projectRequestMan.accept(
				new ProjectRequestParam("/project", id, RequestOperation.SignUp, RequestType.Enquiry));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockEnquiryMan).processEnquiryResponse(argument.capture(), any(),
				eq(RegistrationRequestAction.accept), any(), any());
		assertThat(argument.getValue()).isEqualTo(id);
	}

	@Test
	public void shouldForwardDeclineRegistrationRequestToCoreManager() throws EngineException
	{
		String id = "1";
		RegistrationRequestState state = new RegistrationRequestState();
		state.setRequestId(id);
		when(mockRegistrationMan.getRegistrationRequest(eq(id))).thenReturn(state);

		projectRequestMan.decline(new ProjectRequestParam("/project", id, RequestOperation.SignUp,
				RequestType.Registration));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockRegistrationMan).processRegistrationRequest(argument.capture(), any(),
				eq(RegistrationRequestAction.reject), any(), any());
		assertThat(argument.getValue()).isEqualTo(id);
	}

	@Test
	public void shouldForwardDeclineEnquiryResponsesToCoreManager() throws EngineException
	{
		String id = "1";
		EnquiryResponseState state = new EnquiryResponseState();
		state.setRequestId(id);
		when(mockEnquiryMan.getEnquiryResponse(eq(id))).thenReturn(state);

		projectRequestMan.decline(
				new ProjectRequestParam("/project", id, RequestOperation.Update, RequestType.Enquiry));

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockEnquiryMan).processEnquiryResponse(argument.capture(), any(),
				eq(RegistrationRequestAction.reject), any(), any());
		assertThat(argument.getValue()).isEqualTo(id);
	}

	@Test
	public void shouldGetRegistrationFormLink() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		RegistrationForm form = new RegistrationFormBuilder().withByInvitationOnly(false).withName("regForm")
				.withDefaultCredentialRequirement("").build();
		when(mockRegistrationMan.getForm(eq("regForm"))).thenReturn(form);
		when(mockPublicRegistrationURLSupport.getPublicRegistrationLink(form)).thenReturn("link");

		
		Optional<String> projectRegistrationFormLink = projectRequestMan
				.getProjectRegistrationFormLink("/project");
		assertThat(projectRegistrationFormLink.isPresent()).isEqualTo(true);
	}

	@Test
	public void shouldGetEnquiryFormLink() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		EnquiryForm form = new EnquiryFormBuilder().withByInvitationOnly(false).withName("enqForm")
				.withType(EnquiryType.REQUESTED_OPTIONAL).withTargetGroups(new String[] { "/" })
				.build();
		when(mockEnquiryMan.getEnquiry("enqForm")).thenReturn(form);
		when(mockPublicRegistrationURLSupport.getWellknownEnquiryLink("enqForm")).thenReturn("link");
		
		Optional<String> projectEnquiryFormLink = projectRequestMan.getProjectSignUpEnquiryFormLink("/project");
		assertThat(projectEnquiryFormLink.isPresent()).isEqualTo(true);
	}
}
