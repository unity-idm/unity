package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.ProjectAddInvitationResult;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation;
import pl.edu.icm.unity.engine.api.project.ProjectRestManagementAdapter;
import pl.edu.icm.unity.engine.api.registration.RequestType;

@ExtendWith(MockitoExtension.class)
class RestUpmanWorkflowServiceTest
{
	@Mock
	private DelegatedGroupManagement groups;
	@Mock
	private ProjectInvitationsManagement invitations;
	@Mock
	private ProjectRequestManagement requests;
	@Mock
	private GroupsManagement groupManagement;
	@Mock
	private UpmanRestAuthorizationManager restAuthorization;

	private RestUpmanWorkflowService service;

	@BeforeEach
	void setUp() throws Exception
	{
		Group project = new Group("/root/project");
		project.setDelegationConfiguration(new GroupDelegationConfiguration(true, false, null, null, null,
				null, List.of(), List.of()));
		GroupContents contents = new GroupContents();
		contents.setGroup(project);
		when(groupManagement.getContents(eq("/root/project"), anyInt())).thenReturn(contents);
		ProjectRestManagementAdapter adapter = new ProjectRestManagementAdapter()
		{
			@Override
			public <T> T withRestManagement(String projectPath, Operation<T> operation)
					throws pl.edu.icm.unity.base.exceptions.EngineException
			{
				return operation.run();
			}
		};
		service = new RestUpmanWorkflowService();
		service.init(groups, invitations, requests, groupManagement, adapter, restAuthorization,
				"/root", "/auth");
	}

	@Test
	void shouldReturnEachAvailablePublicFormLink() throws Exception
	{
		when(requests.getProjectRegistrationFormLink("/root/project"))
				.thenReturn(Optional.of("https://example.org/registration"));
		when(requests.getProjectSignUpEnquiryFormLink("/root/project"))
				.thenReturn(Optional.of("https://example.org/signup"));
		when(requests.getProjectUpdateMembershipEnquiryFormLink("/root/project"))
				.thenReturn(Optional.of("https://example.org/update"));

		assertThat(service.registrationFormLink("project").link())
				.isEqualTo("https://example.org/registration");
		assertThat(service.signUpEnquiryLink("project").link())
				.isEqualTo("https://example.org/signup");
		assertThat(service.membershipUpdateEnquiryLink("project").link())
				.isEqualTo("https://example.org/update");
	}

	@Test
	void shouldReportUnavailablePublicFormLinkAsMissing() throws Exception
	{
		when(requests.getProjectRegistrationFormLink("/root/project")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.registrationFormLink("project"))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDispatchBothRequestDecisions() throws Exception
	{
		ProjectRequest pending = new ProjectRequest("pending", RequestOperation.SignUp,
				RequestType.Registration, "/root/project", "Alice", null, Optional.empty(), Instant.now());
		when(requests.getRequests("/root/project")).thenReturn(List.of(pending));

		service.decide("project", "pending", "accept");
		service.decide("project", "pending", "decline");

		ArgumentCaptor<ProjectRequestParam> accepted = ArgumentCaptor.forClass(ProjectRequestParam.class);
		ArgumentCaptor<ProjectRequestParam> declined = ArgumentCaptor.forClass(ProjectRequestParam.class);
		verify(requests).accept(accepted.capture());
		verify(requests).decline(declined.capture());
		assertThat(accepted.getValue().id).isEqualTo("pending");
		assertThat(accepted.getValue().project).isEqualTo("/root/project");
		assertThat(accepted.getValue().operation).isEqualTo(RequestOperation.SignUp);
		assertThat(accepted.getValue().type).isEqualTo(RequestType.Registration);
		assertThat(declined.getValue().id).isEqualTo("pending");
	}

	@Test
	void shouldRejectInvalidRequestDecision()
	{
		assertThatThrownBy(() -> service.decide("project", "pending", "unknown"))
				.isInstanceOf(BadRequestException.class);
		verifyNoInteractions(requests);
	}

	@Test
	void shouldUpdateSubgroupNameAndVisibilityTogether() throws Exception
	{
		GroupContents contents = new GroupContents();
		contents.setGroup(new Group("/root/project/team"));
		when(groupManagement.getContents(eq("/root/project/team"), anyInt())).thenReturn(contents);
		RestGroupUpdateRequest input = new RestGroupUpdateRequest("team", Map.of("en", "Renamed"), true);

		service.updateGroup("project", input);

		ArgumentCaptor<I18nString> displayedName = ArgumentCaptor.forClass(I18nString.class);
		InOrder order = inOrder(groups);
		order.verify(groups).setGroupAccessMode("/root/project", "/root/project/team", true);
		order.verify(groups).setGroupDisplayedName(eq("/root/project"), eq("/root/project/team"),
				displayedName.capture());
		assertThat(displayedName.getValue().getMap()).containsEntry("en", "Renamed");
	}

	@Test
	void shouldReportInvitationOutcomesPerEmailAfterSendFailure() throws Exception
	{
		when(invitations.addInvitations(any())).thenAnswer(invocation -> {
			Set<ProjectInvitationParam> params = invocation.getArgument(0);
			String email = params.iterator().next().contactAddress;
			if (email.equals("failed@example.org"))
				throw new IllegalStateException("send failed");
			return ProjectAddInvitationResult.builder()
					.withProjectAlreadyMemberEmails(email.equals("member@example.org") ? Set.of(email) : Set.of())
					.build();
		});
		RestInvitationRequest input = new RestInvitationRequest(
				List.of("sent@example.org", "member@example.org", "failed@example.org"), List.of(), false,
				Instant.now().plusSeconds(3600));

		List<RestInvitationOutcome> result = service.sendInvitations("project", input);

		assertThat(result).extracting(RestInvitationOutcome::status)
				.containsExactly("sent", "alreadyMember", "failed");
		assertThat(result.get(2).message()).isEqualTo("send failed");
	}

	@Test
	void shouldCleanDelegatedDescendantsBeforeRemovingRegularGroup() throws Exception
	{
		String parent = "/root/project/parent";
		String child = parent + "/child";
		GroupContents parentContents = new GroupContents();
		parentContents.setGroup(new Group(parent));
		when(groupManagement.getContents(eq(parent), anyInt())).thenReturn(parentContents);
		GroupDelegationConfiguration regular = new GroupDelegationConfiguration(false, false, null, null,
				null, null, List.of(), List.of());
		GroupDelegationConfiguration delegated = new GroupDelegationConfiguration(true, false, null, null,
				null, null, List.of(), List.of());
		when(groups.getGroupAndSubgroups("/root/project", parent)).thenReturn(Map.of(
				parent, new DelegatedGroupContents(new DelegatedGroup(parent, regular, false,
						new I18nString("Parent")), Optional.empty()),
				child, new DelegatedGroupContents(new DelegatedGroup(child, delegated, false,
						new I18nString("Child")), Optional.empty())));

		service.removeGroup("project", "parent");

		InOrder order = inOrder(groups);
		order.verify(groups).removeProject("/root/project", child);
		order.verify(groups).removeGroup("/root/project", parent);
	}

	@Test
	void shouldValidateAllInvitationAddressesBeforeSending()
	{
		RestInvitationRequest input = new RestInvitationRequest(List.of("valid@example.org", "invalid"),
				List.of(), false, Instant.now().plusSeconds(3600));

		assertThatThrownBy(() -> service.sendInvitations("project", input))
				.isInstanceOf(BadRequestException.class);
		verifyNoInteractions(invitations);
	}
}
