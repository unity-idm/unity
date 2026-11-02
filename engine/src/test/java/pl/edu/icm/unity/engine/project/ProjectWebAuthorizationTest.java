/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.manager;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.projectsAdmin;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.regular;
import static pl.edu.icm.unity.engine.project.ProjectAuthorizationRoleAttributeTypeProvider
		.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(MockitoExtension.class)
class ProjectWebAuthorizationTest
{
	private static final String PROJECT = "/project";
	private static final long CLIENT_ID = 1L;

	@Mock
	private AttributeDAO attributeDao;

	private ProjectWebAuthorization authorization;
	private InvocationContext previousContext;

	@BeforeEach
	void setUp()
	{
		previousContext = InvocationContext.hasCurrent() ? InvocationContext.getCurrent() : null;
		setAuthenticatedContext();
		authorization = new ProjectWebAuthorization(attributeDao);
	}

	@AfterEach
	void restoreInvocationContext()
	{
		InvocationContext.setCurrent(previousContext);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("ordinaryAuthorizationCases")
	void shouldAuthorizeAccordingToProjectRole(AuthorizationCase testCase)
	{
		setRoleAttributes(testCase.roleAttributes());

		Throwable failure = catchThrowable(() -> authorize(testCase));

		assertAuthorizationResult(failure, testCase.authorized());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("roleAssignmentCases")
	void shouldAuthorizeRoleAssignmentAccordingToProjectRole(AuthorizationCase testCase)
	{
		setRoleAttributes(testCase.roleAttributes());

		Throwable failure = catchThrowable(() -> authorize(testCase));

		assertAuthorizationResult(failure, testCase.authorized());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("roleAttributeCases")
	void shouldReadAllProjectRoleAttributeValues(AuthorizationCase testCase)
	{
		setRoleAttributes(testCase.roleAttributes());

		Throwable failure = catchThrowable(() -> authorize(testCase));

		assertAuthorizationResult(failure, testCase.authorized());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("authorizationChecks")
	void shouldRejectUnauthenticatedClient(AuthorizationCase testCase)
	{
		InvocationContext.getCurrent().setLoginSession(null);

		assertThatThrownBy(() -> authorize(testCase)).isInstanceOf(AuthorizationException.class)
				.hasMessageContaining("not authenticated");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("authorizationChecks")
	void shouldRejectOutdatedCredential(AuthorizationCase testCase)
	{
		InvocationContext.getCurrent().getLoginSession().setOutdatedCredentialId("password");

		assertThatThrownBy(() -> authorize(testCase)).isInstanceOf(AuthorizationException.class)
				.hasMessageContaining("outdated");
	}

	@Test
	void shouldAllowListingManagedProjectsForAuthenticatedCaller() throws AuthorizationException
	{
		authorization.assertManagedProjectsListingAuthorization();
	}

	@Test
	void shouldReportRoleLookupFailure()
	{
		when(attributeDao.getAttributes(PROJECT_MANAGEMENT_AUTHORIZATION_ROLE, CLIENT_ID, PROJECT))
				.thenThrow(new IllegalStateException("storage unavailable"));

		assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
				.isInstanceOf(InternalException.class).hasMessageContaining("entity " + CLIENT_ID);
	}

	private void authorize(AuthorizationCase testCase) throws AuthorizationException
	{
		switch (testCase.check())
		{
		case MANAGED_PROJECTS_LISTING -> authorization.assertManagedProjectsListingAuthorization();
		case MANAGER -> authorization.assertManagerAuthorization(testCase.projectPath());
		case PROJECTS_ADMIN -> authorization.assertProjectsAdminAuthorization(testCase.projectPath());
		case ROLE_MANAGER -> authorization.assertRoleManagerAuthorization(
				testCase.projectPath(), testCase.groupPath(), testCase.requestedRole());
		}
	}

	private void setRoleAttributes(List<List<String>> values)
	{
		List<StoredAttribute> attributes = values.stream()
				.map(roles -> new StoredAttribute(new AttributeExt(new Attribute(
						PROJECT_MANAGEMENT_AUTHORIZATION_ROLE, "string", PROJECT, roles), false), CLIENT_ID))
				.toList();
		when(attributeDao.getAttributes(PROJECT_MANAGEMENT_AUTHORIZATION_ROLE, CLIENT_ID, PROJECT))
				.thenReturn(attributes);
	}

	private static void setAuthenticatedContext()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession("1", null, null, 100, CLIENT_ID, null, null, null, null));
		InvocationContext.setCurrent(context);
	}

	private static void assertAuthorizationResult(Throwable failure, boolean authorized)
	{
		if (authorized)
			assertThat(failure).isNull();
		else
			assertThat(failure).isInstanceOf(AuthorizationException.class);
	}

	private static Stream<AuthorizationCase> authorizationChecks()
	{
		return Arrays.stream(AuthorizationCheck.values())
				.map(check -> AuthorizationCase.builder().withCheck(check).build());
	}

	private static Stream<AuthorizationCase> ordinaryAuthorizationCases()
	{
		return Stream.of(AuthorizationCheck.MANAGER,
				AuthorizationCheck.PROJECTS_ADMIN).flatMap(check -> Stream.of(
				AuthorizationCase.builder().withCheck(check).withRoles().build(),
				AuthorizationCase.builder().withCheck(check).withRoles(regular).build(),
				AuthorizationCase.builder().withCheck(check).withRoles(manager)
						.withAuthorized(check != AuthorizationCheck.PROJECTS_ADMIN).build(),
				AuthorizationCase.builder().withCheck(check).withRoles(projectsAdmin).withAuthorized(true).build()));
	}

	private static Stream<AuthorizationCase> roleAssignmentCases()
	{
		return Stream.concat(Stream.concat(
				roleAssignments(manager, PROJECT, List.of(regular, manager)),
				Stream.of(PROJECT + "/sub", PROJECT + "/sub/sub2")
						.flatMap(path -> roleAssignments(manager, path, List.of()))),
				Stream.of(PROJECT, PROJECT + "/sub", PROJECT + "/sub/sub2").flatMap(path -> Stream.concat(
						roleAssignments(projectsAdmin, path, List.of(GroupAuthorizationRole.values())),
						roleAssignments(regular, path, List.of()))));
	}

	private static Stream<AuthorizationCase> roleAssignments(GroupAuthorizationRole actorRole, String groupPath,
			List<GroupAuthorizationRole> permittedRoles)
	{
		return Arrays.stream(GroupAuthorizationRole.values()).map(role -> AuthorizationCase.builder()
				.withCheck(AuthorizationCheck.ROLE_MANAGER).withRoles(actorRole).withGroupPath(groupPath)
				.withRequestedRole(role).withAuthorized(permittedRoles.contains(role)).build());
	}

	private static Stream<AuthorizationCase> roleAttributeCases()
	{
		return Stream.of(
				AuthorizationCase.builder().withRoleAttributes(List.of()).build(),
				AuthorizationCase.builder().withRoleAttributes(List.of(List.of())).build(),
				AuthorizationCase.builder().withRoles(regular, manager).withAuthorized(true).build(),
				AuthorizationCase.builder().withRoles(regular, projectsAdmin).withAuthorized(true).build(),
				AuthorizationCase.builder()
						.withRoleAttributes(List.of(List.of(regular.name()), List.of(manager.name())))
						.withAuthorized(true).build(),
				AuthorizationCase.builder().withRoleAttributes(List.of(List.of(), List.of(projectsAdmin.name())))
						.withAuthorized(true).build());
	}

	private enum AuthorizationCheck
	{
		MANAGER, PROJECTS_ADMIN, ROLE_MANAGER, MANAGED_PROJECTS_LISTING
	}

	private record AuthorizationCase(AuthorizationCheck check, String projectPath, String groupPath,
			GroupAuthorizationRole requestedRole, List<List<String>> roleAttributes, boolean authorized)
	{
		private static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private AuthorizationCheck check = AuthorizationCheck.MANAGER;
			private String projectPath = PROJECT;
			private String groupPath = PROJECT + "/sub";
			private GroupAuthorizationRole requestedRole = manager;
			private List<List<String>> roleAttributes = List.of();
			private boolean authorized;

			private Builder withCheck(AuthorizationCheck check)
			{
				this.check = check;
				return this;
			}

			private Builder withGroupPath(String groupPath)
			{
				this.groupPath = groupPath;
				return this;
			}

			private Builder withRequestedRole(GroupAuthorizationRole requestedRole)
			{
				this.requestedRole = requestedRole;
				return this;
			}

			private Builder withRoles(GroupAuthorizationRole... roles)
			{
				return withRoleAttributes(List.of(Arrays.stream(roles).map(Enum::name).toList()));
			}

			private Builder withRoleAttributes(List<List<String>> roleAttributes)
			{
				this.roleAttributes = roleAttributes;
				return this;
			}

			private Builder withAuthorized(boolean authorized)
			{
				this.authorized = authorized;
				return this;
			}

			private AuthorizationCase build()
			{
				return new AuthorizationCase(check, projectPath, groupPath, requestedRole, roleAttributes, authorized);
			}
		}
	}

}
