/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;

import io.imunity.upman.utils.DelegatedGroupsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestType;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TestRequestsController
{
	@Mock
	private UnityMessageSource mockMsg;

	@Mock
	private ProjectRequestManagement mockRequestMan;

	@Mock
	private DelegatedGroupsHelper mockDelGroupHelper;

	private UpdateRequestsController controller;

	@Before
	public void initController()
	{
		controller = new UpdateRequestsController(mockMsg, mockRequestMan, mockDelGroupHelper);
	}

	@Test
	public void shouldForwardGetRequestsToCoreManager() throws ControllerException, EngineException
	{
		controller.getUpdateRequests("/project");
		verify(mockRequestMan).getRequests(eq("/project"));
	}

	@Test
	public void shouldForwardAcceptToCoreManager() throws ControllerException, EngineException
	{
		controller.accept("/project", Sets.newHashSet(getRequest()));

		ArgumentCaptor<ProjectRequestParam> argument = ArgumentCaptor.forClass(ProjectRequestParam.class);
		verify(mockRequestMan).accept((argument.capture()));
		assertThat(argument.getValue().project, is("/project"));
		assertThat(argument.getValue().id, is("id"));
		assertThat(argument.getValue().type, is(RequestType.Registration));
		assertThat(argument.getValue().operation, is(RequestOperation.SignUp));
	}

	@Test
	public void shouldForwardDeclineRequestsToCoreManager() throws ControllerException, EngineException
	{
		controller.decline("/project", Sets.newHashSet(getRequest()));

		ArgumentCaptor<ProjectRequestParam> argument = ArgumentCaptor.forClass(ProjectRequestParam.class);
		verify(mockRequestMan).decline((argument.capture()));
		assertThat(argument.getValue().project, is("/project"));
		assertThat(argument.getValue().id, is("id"));
		assertThat(argument.getValue().type, is(RequestType.Registration));
		assertThat(argument.getValue().operation, is(RequestOperation.SignUp));
	}
	
	private UpdateRequestEntry getRequest()
	{
		return new UpdateRequestEntry("id", RequestOperation.SignUp, RequestType.Registration,
				new VerifiableElementBase("demo@demo.com"), "name", null, null);
	}

	@Test
	public void shouldForwardGetRegLinkToCoreManager() throws ControllerException, EngineException
	{
		controller.getProjectRegistrationFormLink("/project");
		verify(mockRequestMan).getProjectRegistrationFormLink(eq("/project"));

	}

	@Test
	public void shouldForwardGetEnqLinkToCoreManager() throws ControllerException, EngineException
	{
		controller.getProjectSingUpEnquiryFormLink("/project");
		verify(mockRequestMan).getProjectSignUpEnquiryFormLink(eq("/project"));

	}
}
