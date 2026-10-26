package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.ProjectAuthorizationFactory;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.engine.api.project.ProjectManagementFactory;
import pl.edu.icm.unity.engine.api.project.ProjectManagementFactory.ProjectManagement;
import pl.edu.icm.unity.engine.api.project.ProjectRequestManagement;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

class ProjectAuthorizationIntegrationTest extends DBIntegrationTestBase
{
	private static final String PROJECT = "/A/project";

	@Autowired
	private ProjectManagementFactory projects;
	@Autowired
	private ProjectAuthorizationFactory authorizations;
	@Autowired
	private DelegatedGroupManagement webGroups;
	@Autowired
	private ProjectInvitationsManagement webInvitations;
	@Autowired
	private ProjectRequestManagement webRequests;

	private ProjectManagement rest;

	@BeforeEach
	void configureProjects() throws Exception
	{
		groupsMan.addGroup(new Group("/A"));
		addProject(PROJECT, true, true);
		addProject("/A/other", true, true);
		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		attrsMan.createAttribute(new EntityParam(entityId),
				new Attribute("sys:ProjectManagementRESTAPIRole", "enumeration", "/", List.of("manager")));
		rest = projects.create(authorizations.createRest("/A", "/"));
	}

	@Test
	void shouldKeepRestAndWebAuthorizationIndependent() throws Exception
	{
		assertWebAccessDenied();

		String subgroup = rest.groups().addGroup(PROJECT, PROJECT, new I18nString("Team"), false);

		assertThat(rest.groups().getContents(PROJECT, subgroup).group.path).isEqualTo(subgroup);
		assertThat(rest.invitations().getInvitations(PROJECT)).isEmpty();
		assertThat(rest.requests().getRequests(PROJECT)).isEmpty();
		assertWebAccessDenied();
		assertThat(AopUtils.isAopProxy(rest.groups())).isTrue();
		assertThat(AopUtils.isAopProxy(rest.invitations())).isTrue();
		assertThat(AopUtils.isAopProxy(rest.requests())).isTrue();
	}

	@Test
	void shouldEnforceExactOAuthScopeAtEveryProjectService() throws Exception
	{
		InvocationContext.getCurrent().setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.getCurrent().setScopes(List.of("sys:upman-api:project"));

		assertThat(rest.groups().getContents(PROJECT, PROJECT).group.path).isEqualTo(PROJECT);
		assertThat(rest.invitations().getInvitations(PROJECT)).isEmpty();
		assertThat(rest.requests().getRequests(PROJECT)).isEmpty();
		assertThatThrownBy(() -> rest.groups().getContents("/A/other", "/A/other"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> rest.invitations().getInvitations("/A/other"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> rest.requests().getRequests("/A/other"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> rest.groups().addGroup("/A/other", "/A/other", new I18nString("Denied"), false))
				.isInstanceOf(AuthorizationException.class);
		assertWebAccessDenied();
	}

	@Test
	void shouldKeepEndpointPoliciesIndependent() throws Exception
	{
		groupsMan.addGroup(new Group("/B"));
		addProject("/B/project", true, true);
		ProjectManagement otherEndpoint = projects.create(authorizations.createRest("/B", "/"));
		InvocationContext.getCurrent().setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.getCurrent().setScopes(List.of("sys:upman-api:project"));

		assertThat(rest.groups().getContents(PROJECT, PROJECT).group.path).isEqualTo(PROJECT);
		assertThat(otherEndpoint.groups().getContents("/B/project", "/B/project").group.path)
				.isEqualTo("/B/project");
		assertThatThrownBy(() -> rest.groups().getContents("/B/project", "/B/project"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("configured root");
		assertThatThrownBy(() -> otherEndpoint.groups().getContents(PROJECT, PROJECT))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("configured root");
	}

	@Test
	void shouldApplyPolicyToCurrentUserProjectDiscovery() throws Exception
	{
		groupsMan.addGroup(new Group("/B"));
		addProject("/B/project", true, true);
		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		EntityParam entity = new EntityParam(entityId);
		groupsMan.addMemberFromParent("/A", entity);
		groupsMan.addMemberFromParent("/B", entity);
		for (String path : List.of(PROJECT, "/A/other", "/B/project"))
		{
			groupsMan.addMemberFromParent(path, entity);
			attrsMan.createAttribute(entity,
					new Attribute("sys:ProjectManagementRole", "enumeration", path, List.of("manager")));
		}
		InvocationContext.getCurrent().setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.getCurrent().setScopes(List.of("sys:upman-api:project"));

		assertThat(rest.groups().getManagedProjectsForCurrentUser()).extracting(project -> project.path)
				.containsExactly(PROJECT);
		InvocationContext.getCurrent().setScopes(List.of(UpmanSystemScopeProvider.API_SCOPE));
		assertThat(rest.groups().getManagedProjectsForCurrentUser()).extracting(project -> project.path)
				.containsExactlyInAnyOrder(PROJECT, "/A/other");
		assertThat(webGroups.getManagedProjectsForCurrentUser()).extracting(project -> project.path)
				.containsExactlyInAnyOrder(PROJECT, "/A/other", "/B/project");
	}

	@Test
	void shouldListProjectsForTheCurrentCaller() throws Exception
	{
		Identity otherManager = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "another-project-manager"),
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid);
		EntityParam otherManagerEntity = new EntityParam(otherManager);
		groupsMan.addMemberFromParent("/A", otherManagerEntity);
		groupsMan.addMemberFromParent(PROJECT, otherManagerEntity);
		attrsMan.createAttribute(otherManagerEntity,
				new Attribute("sys:ProjectManagementRole", "enumeration", PROJECT, List.of("manager")));
		assertThat(webGroups.getManagedProjectsForCurrentUser()).isEmpty();

		InvocationContext previousContext = InvocationContext.getCurrent();
		InvocationContext otherManagerContext = new InvocationContext(null, null, List.of());
		otherManagerContext.setLoginSession(new LoginSession(
				null, null, 0, otherManager.getEntityId(), null, null, null, null));
		InvocationContext.setCurrent(otherManagerContext);
		try
		{
			assertThat(webGroups.getManagedProjectsForCurrentUser()).extracting(project -> project.path)
					.containsExactly(PROJECT);
		} finally
		{
			InvocationContext.setCurrent(previousContext);
		}
		assertThat(webGroups.getManagedProjectsForCurrentUser()).isEmpty();
	}

	@Test
	void shouldEnforceDomainConstraintsWithRestAuthorization() throws Exception
	{
		addProject("/A/disabled", false, true);
		addProject("/A/no-subprojects", true, false);

		assertThatThrownBy(() -> rest.groups().getContents(PROJECT, "/A/other"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not child");
		assertThatThrownBy(() -> rest.requests().getRequests("/A/disabled"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("enabled delegation");
		assertThatThrownBy(() -> rest.groups().removeProject("/A/no-subprojects", "/A/no-subprojects/sub"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("subprojects creation");
		assertThatThrownBy(() -> rest.invitations().addInvitations(Set.of(new ProjectInvitationParam(
				PROJECT, "user@example.org", List.of("/A/other"), false, Instant.now().plusSeconds(3600)))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not child");
	}

	private void assertWebAccessDenied()
	{
		assertThatThrownBy(() -> webGroups.getContents(PROJECT, PROJECT))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> webInvitations.getInvitations(PROJECT))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> webRequests.getRequests(PROJECT))
				.isInstanceOf(AuthorizationException.class);
	}

	private void addProject(String path, boolean delegation, boolean subprojects) throws Exception
	{
		Group project = new Group(path);
		project.setDelegationConfiguration(new GroupDelegationConfiguration(delegation, subprojects,
				null, null, null, null, List.of(), List.of()));
		groupsMan.addGroup(project);
	}
}
