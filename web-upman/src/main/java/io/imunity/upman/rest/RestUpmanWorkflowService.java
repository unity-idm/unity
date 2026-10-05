package io.imunity.upman.rest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequest;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestParam;
import pl.edu.icm.unity.engine.api.project.ProjectRestManagementAdapter;
import pl.edu.icm.unity.engine.api.project.SubprojectGroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
class RestUpmanWorkflowService
{
	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private DelegatedGroupManagement groups;
	private ProjectInvitationsManagement invitations;
	private ProjectRequestManagement requests;
	private GroupsManagement groupManagement;
	private ProjectRestManagementAdapter projectAuthorization;
	private UpmanRestAuthorizationManager restAuthorization;
	private String rootGroup;
	private String authorizationGroup;

	void init(DelegatedGroupManagement groups, ProjectInvitationsManagement invitations,
			ProjectRequestManagement requests, GroupsManagement groupManagement,
			ProjectRestManagementAdapter projectAuthorization, UpmanRestAuthorizationManager restAuthorization,
			String rootGroup, String authorizationGroup)
	{
		this.groups = groups;
		this.invitations = invitations;
		this.requests = requests;
		this.groupManagement = groupManagement;
		this.projectAuthorization = projectAuthorization;
		this.restAuthorization = restAuthorization;
		this.rootGroup = rootGroup;
		this.authorizationGroup = authorizationGroup;
	}

	private String project(String projectId) throws EngineException
	{
		restAuthorization.assertProjectAuthorization(authorizationGroup, projectId);
		String path = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		new ProjectGroupProvider(groupManagement).getProjectGroup(projectId, path);
		return path;
	}

	private String resolveGroup(String projectPath, String relativePath) throws EngineException
	{
		String path = ProjectPathProvider.resolveGroupPath(projectPath, relativePath);
		try
		{
			groupManagement.getContents(path, GroupContents.METADATA);
		} catch (GroupNotFoundException e)
		{
			throw new NotFoundException("Group not found", e);
		}
		return path;
	}

	private String relative(String projectPath, String groupPath)
	{
		if (groupPath.equals(projectPath))
			return "";
		if (!groupPath.startsWith(projectPath + "/"))
			throw new BadRequestException("Group is outside the project");
		return groupPath.substring(projectPath.length() + 1);
	}

	private I18nString name(Map<String, String> values)
	{
		if (values == null || values.isEmpty() || values.values().stream().anyMatch(v -> v == null || v.isBlank()))
			throw new BadRequestException("A displayed name is required");
		I18nString name = new I18nString();
		name.addAllValues(values);
		if (values.containsKey(""))
			name.setDefaultValue(values.get(""));
		return name;
	}

	@Transactional
	List<RestInvitationOutcome> sendInvitations(String projectId, RestInvitationRequest input) throws EngineException
	{
		String project = project(projectId);
		if (input == null || input.emails() == null || input.emails().isEmpty()
				|| input.expiration() == null || !input.expiration().isAfter(Instant.now()))
			throw new BadRequestException("Emails and a future expiration are required");
		if (input.groups() != null && input.groups().stream().anyMatch(path -> path == null))
			throw new BadRequestException("Invalid group");
		List<String> selectedGroups = input.groups() == null ? List.of() : input.groups().stream()
				.map(path -> {
					try
					{
						return resolveGroup(project, path);
					} catch (EngineException e)
					{
						throw new BadRequestException("Invalid group", e);
					}
				}).toList();
		List<String> emails = input.emails().stream().distinct().toList();
		if (emails.stream().anyMatch(email -> email == null || !EMAIL.matcher(email).matches()))
			throw new BadRequestException("Invalid email address");
		List<RestInvitationOutcome> outcomes = new ArrayList<>();
		for (String email : emails)
		{
			try
			{
				var param = new ProjectInvitationParam(project, email, selectedGroups,
						input.allowModifyGroups(), input.expiration());
				var result = projectAuthorization.withRestManagement(project,
						() -> invitations.addInvitations(Set.of(param)));
				outcomes.add(new RestInvitationOutcome(email,
						result.projectAlreadyMemberEmails.contains(email) ? "alreadyMember" : "sent", null));
			} catch (EngineException | RuntimeException e)
			{
				outcomes.add(new RestInvitationOutcome(email, "failed", e.getMessage()));
			}
		}
		return outcomes;
	}

	List<RestInvitation> invitations(String projectId) throws EngineException
	{
		String project = project(projectId);
		return projectAuthorization.withRestManagement(project, () -> invitations.getInvitations(project)).stream()
				.map(i -> invitation(project, i)).toList();
	}

	RestInvitation invitation(String projectId, String code) throws EngineException
	{
		return invitations(projectId).stream().filter(i -> i.code().equals(code)).findFirst()
				.orElseThrow(() -> new NotFoundException("Invitation not found"));
	}

	private RestInvitation invitation(String project, ProjectInvitation invitation)
	{
		return new RestInvitation(invitation.registrationCode, invitation.contactAddress,
				invitation.groups.stream().map(path -> relative(project, path)).toList(),
				invitation.allowModifyGroups, invitation.expiration, invitation.lastSentTime,
				invitation.numberOfSends, invitation.link);
	}

	void removeInvitation(String projectId, String code) throws EngineException
	{
		invitation(projectId, code);
		String project = project(projectId);
		projectAuthorization.withRestManagement(project, () -> { invitations.removeInvitation(project, code); return null; });
	}

	void resendInvitation(String projectId, String code) throws EngineException
	{
		invitation(projectId, code);
		String project = project(projectId);
		try
		{
			projectAuthorization.withRestManagement(project,
					() -> { invitations.resendInvitation(project, code); return null; });
		} catch (WrongArgumentException e)
		{
			throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
		}
	}

	void reinvite(String projectId, String code) throws EngineException
	{
		invitation(projectId, code);
		String project = project(projectId);
		projectAuthorization.withRestManagement(project, () -> { invitations.reinvite(project, code); return null; });
	}

	List<RestGroup> groups(String projectId) throws EngineException
	{
		String project = project(projectId);
		Map<String, DelegatedGroupContents> contents = projectAuthorization.withRestManagement(project,
				() -> groups.getGroupAndSubgroups(project, project));
		return contents.values().stream().map(c -> map(project, c)).sorted(Comparator.comparing(RestGroup::path)).toList();
	}

	RestGroup group(String projectId, String relativePath) throws EngineException
	{
		String project = project(projectId);
		String path = resolveGroup(project, relativePath);
		return map(project, projectAuthorization.withRestManagement(project, () -> groups.getContents(project, path)));
	}

	private RestGroup map(String project, DelegatedGroupContents content)
	{
		var group = content.group;
		var delegation = group.delegationConfiguration;
		return new RestGroup(relative(project, group.path), group.displayedName.getMap(), group.open,
				delegation.enabled, delegation.enableSubprojects, delegation.logoUrl,
				content.subGroups.stream().map(path -> relative(project, path)).toList());
	}

	RestGroupPath addGroup(String projectId, RestGroupCreateRequest input) throws EngineException
	{
		String project = project(projectId);
		if (input == null)
			throw new BadRequestException("Group details are required");
		String parent = resolveGroup(project, input.parentPath());
		String path;
		try
		{
			path = projectAuthorization.withRestManagement(project,
					() -> groups.addGroup(project, parent, name(input.displayedName()), input.isPublic()));
		} catch (InternalException e)
		{
			throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
		}
		return new RestGroupPath(relative(project, path));
	}

	@Transactional
	void removeGroup(String projectId, String relativePath) throws EngineException
	{
		String project = project(projectId);
		String path = resolveGroup(project, relativePath);
		if (path.equals(project))
			throw new BadRequestException("Project root cannot be removed as a subgroup");
		projectAuthorization.withRestManagement(project, () -> {
			Map<String, DelegatedGroupContents> descendants = groups.getGroupAndSubgroups(project, path);
			List<String> delegated = descendants.values().stream()
					.filter(content -> content.group.delegationConfiguration.enabled)
					.map(content -> content.group.path)
					.sorted(Comparator.comparingInt(String::length).reversed())
					.toList();
			for (String subproject : delegated)
				groups.removeProject(project, subproject);
			if (!delegated.contains(path))
				groups.removeGroup(project, path);
			return null;
		});
	}

	@Transactional
	void updateGroup(String projectId, RestGroupUpdateRequest input) throws EngineException
	{
		String project = project(projectId);
		if (input == null || input.path() == null || input.path().isBlank() || input.isPublic() == null)
			throw new BadRequestException("Path, displayed name, and visibility are required");
		I18nString displayedName = name(input.displayedName());
		String path = resolveGroup(project, input.path());
		if (path.equals(project))
			throw new BadRequestException("Project root is managed by the project route");
		try
		{
			projectAuthorization.withRestManagement(project, () -> {
				groups.setGroupAccessMode(project, path, input.isPublic());
				groups.setGroupDisplayedName(project, path, displayedName);
				return null;
			});
		} catch (InternalException e)
		{
			throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
		}
	}

	@Transactional
	void setDelegation(String projectId, String relativePath, RestGroupDelegationRequest input) throws EngineException
	{
		if (input == null)
			throw new BadRequestException("Delegation configuration is required");
		String project = project(projectId);
		String path = resolveGroup(project, relativePath);
		if (path.equals(project))
			throw new BadRequestException("Project root delegation is managed by the project route");
		projectAuthorization.withRestManagement(project, () -> {
			groups.setGroupDelegationConfiguration(project, path,
					new SubprojectGroupDelegationConfiguration(input.enabled(), input.enableSubprojects(), input.logoUrl()));
			return null;
		});
	}

	List<RestGroupMember> members(String projectId, String relativePath) throws EngineException
	{
		String project = project(projectId);
		String path = resolveGroup(project, relativePath);
		return projectAuthorization.withRestManagement(project,
				() -> groups.getDelegatedGroupMembers(project, path)).stream().map(this::member).toList();
	}

	private RestGroupMember member(DelegatedGroupMember member)
	{
		return new RestGroupMember(member.entityId, member.name,
				member.email == null ? null : member.email.getValue(), member.role.name(),
				member.attributes.stream().map(a -> new RestAttribute(a.getName(), List.copyOf(a.getValues()))).toList());
	}

	RestGroupMember member(String projectId, String relativePath, long entityId) throws EngineException
	{
		return members(projectId, relativePath).stream().filter(m -> m.entityId() == entityId).findFirst()
				.orElseThrow(() -> new NotFoundException("Member not found"));
	}

	void addMember(String projectId, String relativePath, long entityId) throws EngineException
	{
		String project = project(projectId);
		String path = resolveGroup(project, relativePath);
		projectAuthorization.withRestManagement(project, () -> {
			groups.addMemberToGroup(project, path, entityId);
			return null;
		});
	}

	void removeMember(String projectId, String relativePath, long entityId) throws EngineException
	{
		member(projectId, relativePath, entityId);
		String project = project(projectId);
		String path = resolveGroup(project, relativePath);
		projectAuthorization.withRestManagement(project, () -> {
			groups.removeMemberFromGroup(project, path, entityId); return null;
		});
	}

	RestAuthorizationRole role(String projectId, String relativePath, long entityId) throws EngineException
	{
		return new RestAuthorizationRole(member(projectId, relativePath, entityId).role());
	}

	void setRole(String projectId, String relativePath, long entityId, RestAuthorizationRole role)
			throws EngineException
	{
		member(projectId, relativePath, entityId);
		if (role == null || role.role == null)
			throw new BadRequestException("Role is required");
		GroupAuthorizationRole value;
		try
		{
			value = GroupAuthorizationRole.valueOf(role.role);
		} catch (IllegalArgumentException e)
		{
			throw new BadRequestException("Invalid role", e);
		}
		String project = project(projectId);
		String path = resolveGroup(project, relativePath);
		try
		{
			projectAuthorization.withRestManagement(project, () -> {
				groups.setGroupAuthorizationRole(project, path, entityId, value); return null;
			});
		} catch (InternalException e)
		{
			throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
		}
	}

	List<RestRequest> requests(String projectId) throws EngineException
	{
		String project = project(projectId);
		return projectAuthorization.withRestManagement(project, () -> requests.getRequests(project)).stream()
				.map(r -> request(project, r)).toList();
	}

	RestRequest request(String projectId, String requestId) throws EngineException
	{
		return requests(projectId).stream().filter(r -> r.id().equals(requestId)).findFirst()
				.orElseThrow(() -> new NotFoundException("Pending request not found"));
	}

	private RestRequest request(String project, ProjectRequest request)
	{
		return new RestRequest(request.id, request.operation.name(), request.type.name(), request.name,
				request.email == null ? null : request.email.getValue(),
				request.email == null ? null : request.email.isConfirmed(),
				request.groups.stream().map(path -> relative(project, path)).toList(), request.requestedTime);
	}

	void decide(String projectId, String requestId, String decision) throws EngineException
	{
		String project = project(projectId);
		if (!"accept".equals(decision) && !"decline".equals(decision))
			throw new BadRequestException("Decision must be accept or decline");
		RestRequest request = request(projectId, requestId);
		ProjectRequestParam param = new ProjectRequestParam(project, requestId,
				pl.edu.icm.unity.engine.api.project.ProjectRequestParam.RequestOperation.valueOf(request.operation()),
				pl.edu.icm.unity.engine.api.registration.RequestType.valueOf(request.type()));
		projectAuthorization.withRestManagement(project, () -> {
			if ("accept".equals(decision))
				requests.accept(param);
			else
				requests.decline(param);
			return null;
		});
	}

	RestFormLink registrationFormLink(String projectId) throws EngineException
	{
		return formLink(projectId, requests::getProjectRegistrationFormLink);
	}

	RestFormLink signUpEnquiryLink(String projectId) throws EngineException
	{
		return formLink(projectId, requests::getProjectSignUpEnquiryFormLink);
	}

	RestFormLink membershipUpdateEnquiryLink(String projectId) throws EngineException
	{
		return formLink(projectId, requests::getProjectUpdateMembershipEnquiryFormLink);
	}

	private RestFormLink formLink(String projectId, FormLinkLookup lookup) throws EngineException
	{
		String project = project(projectId);
		Optional<String> link = projectAuthorization.withRestManagement(project, () -> lookup.find(project));
		return new RestFormLink(link.orElseThrow(() -> new NotFoundException("Public form link not found")));
	}

	@FunctionalInterface
	private interface FormLinkLookup
	{
		Optional<String> find(String project) throws EngineException;
	}

	@Component
	static class Factory
	{
		private final ObjectFactory<RestUpmanWorkflowService> factory;
		private final DelegatedGroupManagement groups;
		private final ProjectInvitationsManagement invitations;
		private final ProjectRequestManagement requests;
		private final GroupsManagement groupManagement;
		private final ProjectRestManagementAdapter projectAuthorization;
		private final UpmanRestAuthorizationManager restAuthorization;

		@Autowired
		Factory(ObjectFactory<RestUpmanWorkflowService> factory, DelegatedGroupManagement groups,
				ProjectInvitationsManagement invitations, ProjectRequestManagement requests,
				@Qualifier("insecure") GroupsManagement groupManagement,
				ProjectRestManagementAdapter projectAuthorization, UpmanRestAuthorizationManager restAuthorization)
		{
			this.factory = factory;
			this.groups = groups;
			this.invitations = invitations;
			this.requests = requests;
			this.groupManagement = groupManagement;
			this.projectAuthorization = projectAuthorization;
			this.restAuthorization = restAuthorization;
		}

		RestUpmanWorkflowService newInstance(String rootGroup, String authorizationGroup)
		{
			RestUpmanWorkflowService service = factory.getObject();
			service.init(groups, invitations, requests, groupManagement, projectAuthorization, restAuthorization,
					rootGroup, authorizationGroup);
			return service;
		}
	}
}
