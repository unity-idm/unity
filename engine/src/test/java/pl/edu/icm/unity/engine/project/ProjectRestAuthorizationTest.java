package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.RestProjectAuthorization;

class ProjectRestAuthorizationTest
{
	private final AttributesManagement attributes = mock(AttributesManagement.class);
	private final ProjectRestAuthorization authorization = new ProjectRestAuthorization(attributes, "/root", "/A");
	private InvocationContext context;

	@BeforeEach
	void setUp()
	{
		context = new InvocationContext(null, null, List.of());
		context.setLoginSession(new LoginSession(null, null, 0, 7, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		InvocationContext.setCurrent(context);
	}

	@AfterEach
	void tearDown()
	{
		InvocationContext.setCurrent(null);
	}

	@Test
	void shouldAllowGlobalScopeForEveryProjectAndCreation() throws Exception
	{
		context.setScopes(List.of(RestProjectAuthorization.API_SCOPE));

		authorization.assertProjectCreationAuthorization();
		authorization.assertManagerAuthorization("/root/project-x");
		authorization.assertManagerAuthorization("/root/project-y");
	}

	@Test
	void shouldRestrictProjectScopeToItsExactProject() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:project-x"));

		authorization.assertProjectsDiscoveryAuthorization();
		authorization.assertManagerAuthorization("/root/project-x");
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/root/project-y"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertProjectCreationAuthorization())
				.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void shouldMatchNestedProjectWithoutGrantingItsSibling() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:project-x/child"));

		authorization.assertManagerAuthorization("/root/project-x/child");
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/root/project-x"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/root/project-x/other"))
				.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void shouldRejectOAuthTokenWithoutUpmanScope()
	{
		context.setScopes(List.of("openid"));

		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/root/project-x"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertProjectCreationAuthorization())
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertProjectsDiscoveryAuthorization())
				.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void shouldPreserveManagerRoleAuthorizationForDirectAuthentication() throws Exception
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		AttributeExt role = mock(AttributeExt.class);
		when(role.getValues()).thenReturn(List.of("manager"));
		when(attributes.getAttributes(any(), eq("/A"), eq("sys:ProjectManagementRESTAPIRole")))
				.thenReturn(List.of(role));

		authorization.assertProjectCreationAuthorization();
		authorization.assertManagerAuthorization("/root/project-x");
		authorization.assertProjectsDiscoveryAuthorization();
		authorization.assertProjectsAdminAuthorization("/root/project-x");
		authorization.assertRoleManagerAuthorization("/root/project-x", "/root/project-x/sub",
				GroupAuthorizationRole.projectsAdmin);
	}

	@Test
	void shouldApplyExactScopeToEveryProjectPermission() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:project-x"));
		ProjectRestAuthorization policy = new ProjectRestAuthorization(attributes, "/root", "/A");

		policy.assertManagerAuthorization("/root/project-x");
		policy.assertProjectsAdminAuthorization("/root/project-x");
		policy.assertRoleManagerAuthorization("/root/project-x", "/root/project-x/sub",
				GroupAuthorizationRole.projectsAdmin);
		assertThatThrownBy(() -> policy.assertManagerAuthorization("/root/project-x/child"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policy.assertProjectsAdminAuthorization("/root/project-y"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policy.assertRoleManagerAuthorization("/root/project-y", "/root/project-y",
				GroupAuthorizationRole.manager)).isInstanceOf(AuthorizationException.class);
		verifyNoInteractions(attributes);
	}

	@Test
	void shouldRejectProjectsOutsideEndpointRootWithGlobalScope()
	{
		context.setScopes(List.of(RestProjectAuthorization.API_SCOPE));
		ProjectRestAuthorization policy = new ProjectRestAuthorization(attributes, "/root", "/A");

		assertThatThrownBy(() -> policy.assertManagerAuthorization("/root"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("configured root");
		assertThatThrownBy(() -> policy.assertManagerAuthorization("/root-other/project"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("configured root");
	}

	@Test
	void shouldMatchCanonicalNestedAndLiteralIdsAtServiceBoundary() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:parent/team%west"));
		ProjectRestAuthorization policy = new ProjectRestAuthorization(attributes, "/", "/A");

		policy.assertManagerAuthorization("/parent/team%west");
		assertThatThrownBy(() -> policy.assertManagerAuthorization("/parent/team%west/child"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policy.assertManagerAuthorization("/parent"))
				.isInstanceOf(AuthorizationException.class);
	}

	@Test
	void shouldCheckAuthenticationOnEveryServiceCall() throws Exception
	{
		context.setScopes(List.of(RestProjectAuthorization.API_SCOPE));
		ProjectRestAuthorization policy = new ProjectRestAuthorization(attributes, "/root", "/A");
		policy.assertManagerAuthorization("/root/project-x");

		context.getLoginSession().setOutdatedCredentialId("password");
		assertThatThrownBy(() -> policy.assertManagerAuthorization("/root/project-x"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("outdated");
		context.setLoginSession(null);
		assertThatThrownBy(() -> policy.assertManagerAuthorization("/root/project-x"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("not authenticated");
	}

	@Test
	void shouldFilterManagedProjectListingByEndpointAndToken() throws Exception
	{
		ProjectRestAuthorization policy = new ProjectRestAuthorization(attributes, "/root", "/A");
		context.setScopes(List.of("sys:upman-api:project-x"));

		policy.assertManagedProjectsListingAuthorization();
		assertThat(policy.isProjectVisible("/root/project-x")).isTrue();
		assertThat(policy.isProjectVisible("/root/project-y")).isFalse();
		assertThat(policy.isProjectVisible("/other/project-x")).isFalse();
		context.setScopes(List.of(RestProjectAuthorization.API_SCOPE));
		policy.assertManagedProjectsListingAuthorization();
		assertThat(policy.isProjectVisible("/root/project-y")).isTrue();
		assertThat(policy.isProjectVisible("/other/project-x")).isFalse();
	}

	@Test
	void shouldRejectDirectAuthenticationWithoutRestRoleEvenWithGlobalScope()
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		context.setScopes(List.of(RestProjectAuthorization.API_SCOPE));

		assertEveryOperationDenied(authorization);
	}

	@Test
	void shouldRejectAttributeLookupFailure() throws Exception
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		when(attributes.getAttributes(any(), eq("/A"), eq(RestProjectAuthorization.AUTHORIZATION_ROLE)))
				.thenThrow(new EngineException("lookup failed"));

		assertEveryOperationDenied(authorization);
	}

	@Test
	void shouldRejectMissingAuthenticationForEveryOperation()
	{
		context.setScopes(List.of(RestProjectAuthorization.API_SCOPE));
		context.setLoginSession(null);

		assertEveryOperationDenied(authorization);
		verifyNoInteractions(attributes);
	}

	@Test
	void shouldRejectOutdatedCredentialsForEveryOperation()
	{
		context.setScopes(List.of(RestProjectAuthorization.API_SCOPE));
		context.getLoginSession().setOutdatedCredentialId("password");

		assertEveryOperationDenied(authorization);
		verifyNoInteractions(attributes);
	}

	@Test
	void shouldRejectEmptyProjectScopeWithoutConsultingCallerRoles()
	{
		context.setScopes(List.of(RestProjectAuthorization.PROJECT_SCOPE_PREFIX));

		assertEveryOperationDenied(authorization);
		verifyNoInteractions(attributes);
	}

	@Test
	void shouldUseAuthorizationGroupConfiguredForEachEndpoint() throws Exception
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		AttributeExt role = mock(AttributeExt.class);
		when(role.getValues()).thenReturn(List.of("manager"));
		when(attributes.getAttributes(any(), eq("/A"), eq(RestProjectAuthorization.AUTHORIZATION_ROLE)))
				.thenReturn(List.of(role));
		ProjectRestAuthorization otherEndpoint = new ProjectRestAuthorization(attributes, "/root", "/B");

		authorization.assertManagerAuthorization("/root/project-x");
		assertEveryOperationDenied(otherEndpoint);
	}

	@Test
	void shouldSelectAuthorizationPolicyForEachInvocation() throws Exception
	{
		context.setScopes(List.of("sys:upman-api:project-x"));
		authorization.assertManagerAuthorization("/root/project-x");
		verifyNoInteractions(attributes);

		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		assertEveryOperationDenied(authorization);
		AttributeExt role = mock(AttributeExt.class);
		when(role.getValues()).thenReturn(List.of("manager"));
		when(attributes.getAttributes(any(), eq("/A"), eq(RestProjectAuthorization.AUTHORIZATION_ROLE)))
				.thenReturn(List.of(role));
		authorization.assertManagerAuthorization("/root/project-y");
		authorization.assertProjectCreationAuthorization();

		clearInvocations(attributes);
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		authorization.assertManagerAuthorization("/root/project-x");
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/root/project-y"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(authorization::assertProjectCreationAuthorization).isInstanceOf(AuthorizationException.class);
		verifyNoInteractions(attributes);
	}

	@Test
	void shouldRestrictDirectAuthorizationAndDiscoveryToEndpointRoot()
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);

		assertThat(authorization.isProjectVisible("/root/project-x")).isTrue();
		assertThat(authorization.isProjectVisible("/root")).isFalse();
		assertThat(authorization.isProjectVisible("/root-other/project-x")).isFalse();
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/root"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("configured root");
		assertThatThrownBy(() -> authorization.assertManagerAuthorization("/root-other/project-x"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("configured root");
		verifyNoInteractions(attributes);
	}

	@Test
	void shouldRejectMissingDirectAuthenticationForEveryOperation()
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		context.setLoginSession(null);

		assertEveryOperationDenied(authorization);
		verifyNoInteractions(attributes);
	}

	@Test
	void shouldRejectOutdatedDirectCredentialsForEveryOperation()
	{
		context.setInvocationMaterial(InvocationMaterial.DIRECT);
		context.getLoginSession().setOutdatedCredentialId("password");

		assertEveryOperationDenied(authorization);
		verifyNoInteractions(attributes);
	}

	private void assertEveryOperationDenied(ProjectRestAuthorization policy)
	{
		assertThatThrownBy(policy::assertProjectCreationAuthorization).isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(policy::assertProjectsDiscoveryAuthorization).isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policy.assertManagedProjectsListingAuthorization()).isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policy.assertManagerAuthorization("/root/project-x"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policy.assertProjectsAdminAuthorization("/root/project-x"))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> policy.assertRoleManagerAuthorization("/root/project-x", "/root/project-x/sub",
				GroupAuthorizationRole.projectsAdmin)).isInstanceOf(AuthorizationException.class);
	}
}
