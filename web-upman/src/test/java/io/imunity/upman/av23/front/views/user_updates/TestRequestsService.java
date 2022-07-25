/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.user_updates;

import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation;
import pl.edu.icm.unity.engine.api.registration.RequestType;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class TestRequestsService
{
	@Mock
	private MessageSource mockMsg;
	@Mock
	private ProjectRequestManagement mockRequestMan;
	@Mock
	private DelegatedGroupsHelper mockDelGroupHelper;
	@Mock
	private NotificationPresenter notificationPresenter;

	private UpdateRequestsService service;

	@Before
	public void initController()
	{
		service = new UpdateRequestsService(mockMsg, mockRequestMan, mockDelGroupHelper, notificationPresenter);
	}

	@Test
	public void shouldGetRequests() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		service.getUpdateRequests(project);
		verify(mockRequestMan).getRequests(eq("/project"));
	}

	@Test
	public void shouldAccept() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		service.accept(project, Set.of(getRequest()));

		ArgumentCaptor<ProjectRequestParam> argument = ArgumentCaptor.forClass(ProjectRequestParam.class);
		verify(mockRequestMan).accept((argument.capture()));
		assertThat(argument.getValue().project, is("/project"));
		assertThat(argument.getValue().id, is("id"));
		assertThat(argument.getValue().type, is(RequestType.Registration));
		assertThat(argument.getValue().operation, is(RequestOperation.SignUp));
	}

	@Test
	public void shouldDeclineRequests() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		service.decline(project, Set.of(getRequest()));

		ArgumentCaptor<ProjectRequestParam> argument = ArgumentCaptor.forClass(ProjectRequestParam.class);
		verify(mockRequestMan).decline((argument.capture()));
		assertThat(argument.getValue().project, is("/project"));
		assertThat(argument.getValue().id, is("id"));
		assertThat(argument.getValue().type, is(RequestType.Registration));
		assertThat(argument.getValue().operation, is(RequestOperation.SignUp));
	}
	
	private UpdateRequestModel getRequest()
	{
		return UpdateRequestModel.builder()
				.id("id")
				.operation(RequestOperation.SignUp)
				.type(RequestType.Registration)
				.email(new VerifiableElementBase("demo@demo.com"))
				.name("name")
				.build();
	}

	@Test
	public void shouldGetRegLink() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		service.getProjectRegistrationFormLink(project);

		verify(mockRequestMan).getProjectRegistrationFormLink(eq("/project"));

	}

	@Test
	public void shouldGetEnqLink() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		service.getProjectSingUpEnquiryFormLink(project);

		verify(mockRequestMan).getProjectSignUpEnquiryFormLink(eq("/project"));
	}
}
