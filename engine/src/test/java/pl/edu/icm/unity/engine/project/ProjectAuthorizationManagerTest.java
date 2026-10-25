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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(MockitoExtension.class)
class ProjectAuthorizationManagerTest
{
	private static final String PROJECT = "/project";
	private static final long CLIENT_ID = 1L;

	@Mock
	private GroupDAO groupDao;
	@Mock
	private AttributeDAO attributeDao;

	private ProjectAuthorizationManager authorization;
	private InvocationContext previousContext;

	@BeforeEach
	void setUp()
	{
		previousContext = InvocationContext.hasCurrent() ? InvocationContext.getCurrent() : null;
		setAuthenticatedContext();
		authorization = new ProjectAuthorizationManager(groupDao, attributeDao);
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
		configureGroups(testCase);
		setRoleAttributes(testCase.roleAttributes());

		Throwable failure = catchThrowable(() -> authorize(testCase));

		assertAuthorizationResult(failure, testCase.authorized());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("roleAssignmentCases")
	void shouldAuthorizeRoleAssignmentAccordingToProjectRole(AuthorizationCase testCase)
	{
		configureGroups(testCase);
		setRoleAttributes(testCase.roleAttributes());

		Throwable failure = catchThrowable(() -> authorize(testCase));

		assertAuthorizationResult(failure, testCase.authorized());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("roleAttributeCases")
	void shouldReadAllProjectRoleAttributeValues(AuthorizationCase testCase)
	{
		configureGroups(testCase);
		setRoleAttributes(testCase.roleAttributes());

		Throwable failure = catchThrowable(() -> authorize(testCase));

		assertAuthorizationResult(failure, testCase.authorized());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("restAuthorizationCases")
	void shouldBypassProjectRoleChecksWithinRestScope(AuthorizationCase testCase) throws EngineException
	{
		configureGroups(testCase);
		assertThatThrownBy(() -> authorize(testCase)).isInstanceOf(AuthorizationException.class);

		authorization.withRestManagement(PROJECT, () ->
		{
			authorize(testCase);
			return null;
		});

		assertThatThrownBy(() -> authorize(testCase)).isInstanceOf(AuthorizationException.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("differentProjects")
	void shouldRestrictRestScopeToExactProject(AuthorizationCase testCase) throws EngineException
	{
		configureGroups(testCase);

		authorization.withRestManagement(PROJECT, () ->
		{
			assertThatThrownBy(() -> authorize(testCase)).isInstanceOf(AuthorizationException.class);
			return null;
		});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("authorizationChecks")
	void shouldRejectUnauthenticatedClientWithinRestScope(AuthorizationCase testCase)
	{
		InvocationContext.getCurrent().setLoginSession(null);

		assertThatThrownBy(() -> authorization.withRestManagement(PROJECT, () ->
		{
			authorize(testCase);
			return null;
		})).isInstanceOf(AuthorizationException.class).hasMessageContaining("not authenticated");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("authorizationChecks")
	void shouldRejectOutdatedCredentialWithinRestScope(AuthorizationCase testCase)
	{
		InvocationContext.getCurrent().getLoginSession().setOutdatedCredentialId("password");

		assertThatThrownBy(() -> authorization.withRestManagement(PROJECT, () ->
		{
			authorize(testCase);
			return null;
		})).isInstanceOf(AuthorizationException.class).hasMessageContaining("outdated");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("authorizationChecks")
	void shouldRejectDisabledProjectDelegationWithinRestScope(AuthorizationCase testCase)
	{
		addGroup(PROJECT, false, true);

		assertThatThrownBy(() -> authorization.withRestManagement(PROJECT, () ->
		{
			authorize(testCase);
			return null;
		})).isInstanceOf(AuthorizationException.class)
				.hasMessageContaining("enabled delegation").hasMessageContaining(PROJECT);
	}

	@Test
	void shouldRejectDisabledDelegationWithoutRestScope()
	{
		addGroup(PROJECT, false, false);

		assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("enabled delegation");
	}

	@Test
	void shouldRejectDisabledSubprojectsWithinRestScope()
	{
		addGroup(PROJECT, true, false);

		assertThatThrownBy(() -> authorization.withRestManagement(PROJECT, () ->
		{
			authorization.assertProjectsAdminAuthorization(PROJECT, PROJECT + "/sub");
			return null;
		})).isInstanceOf(AuthorizationException.class).hasMessageContaining("subprojects creation");
	}

	@Test
	void shouldRejectDisabledSubprojectsWithoutRestScope()
	{
		addGroup(PROJECT, true, false);

		assertThatThrownBy(() -> authorization.assertProjectsAdminAuthorization(PROJECT, PROJECT + "/sub"))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("subprojects creation");
	}

	@Test
	void shouldRejectDisabledTargetDelegationWithinRestScope()
	{
		addGroup(PROJECT, true, true);
		addGroup(PROJECT + "/sub", false, false);

		assertThatThrownBy(() -> authorization.withRestManagement(PROJECT, () ->
		{
			authorization.assertRoleManagerAuthorization(PROJECT, PROJECT + "/sub", manager);
			return null;
		})).isInstanceOf(AuthorizationException.class)
				.hasMessageContaining("enabled delegation on /project/sub");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("groupContainmentCases")
	void shouldCheckGroupContainmentWithinRestScope(AuthorizationCase testCase)
	{
		configureGroups(testCase);

		Throwable failure = catchThrowable(() -> authorization.withRestManagement(PROJECT, () ->
		{
			authorize(testCase);
			return null;
		}));

		assertGroupContainmentResult(failure, testCase);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("groupContainmentCases")
	void shouldCheckGroupContainmentWithoutRestScope(AuthorizationCase testCase)
	{
		configureGroups(testCase);
		if (testCase.authorized() || testCase.check() == AuthorizationCheck.MANAGER_WITH_GROUP)
			setRoleAttributes(List.of(List.of(projectsAdmin.name())));

		Throwable failure = catchThrowable(() -> authorize(testCase));

		assertGroupContainmentResult(failure, testCase);
	}

	@Test
	void shouldReturnOperationResultAndClearRestScope() throws EngineException
	{
		addGroup(PROJECT, true, true);
		Object expectedResult = new Object();
		AtomicInteger operationCalls = new AtomicInteger();

		Object result = authorization.withRestManagement(PROJECT, () ->
		{
			operationCalls.incrementAndGet();
			authorization.assertManagerAuthorization(PROJECT);
			return expectedResult;
		});

		assertThat(result).isSameAs(expectedResult);
		assertThat(operationCalls.get()).isEqualTo(1);
		assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
				.isInstanceOf(AuthorizationException.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("operationFailures")
	void shouldPropagateFailureAndClearRestScope(ScopeFailureCase testCase)
	{
		addGroup(PROJECT, true, true);

		Throwable failure = catchThrowable(() -> authorization.withRestManagement(PROJECT, () ->
		{
			authorization.assertManagerAuthorization(PROJECT);
			return throwOperationFailure(testCase.failure());
		}));

		assertThat(failure).isSameAs(testCase.failure());
		assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
				.isInstanceOf(AuthorizationException.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("nestedProjects")
	void shouldRestoreOuterRestScopeAfterNestedOperation(AuthorizationCase testCase) throws EngineException
	{
		configureNestedGroups(testCase.projectPath());

		authorization.withRestManagement(PROJECT, () ->
		{
			authorization.assertManagerAuthorization(PROJECT);
			authorization.withRestManagement(testCase.projectPath(), () ->
			{
				assertInnerScope(testCase.projectPath());
				return null;
			});
			assertOuterScope(testCase.projectPath());
			return null;
		});

		assertNoRestScope(testCase.projectPath());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("nestedOperationFailures")
	void shouldRestoreOuterRestScopeAfterNestedFailure(ScopeFailureCase testCase) throws EngineException
	{
		configureNestedGroups(testCase.projectPath());

		authorization.withRestManagement(PROJECT, () ->
		{
			Throwable failure = catchThrowable(() -> authorization.withRestManagement(testCase.projectPath(), () ->
			{
				assertInnerScope(testCase.projectPath());
				return throwOperationFailure(testCase.failure());
			}));
			assertThat(failure).isSameAs(testCase.failure());
			assertOuterScope(testCase.projectPath());
			return null;
		});

		assertNoRestScope(testCase.projectPath());
	}

	@Test
	void shouldIsolateRestScopeBetweenThreads() throws Exception
	{
		addGroup(PROJECT, true, true);
		CountDownLatch scopeEntered = new CountDownLatch(1);
		CountDownLatch releaseScope = new CountDownLatch(1);
		var executor = Executors.newSingleThreadExecutor();
		try
		{
			var scopedOperation = executor.submit(() ->
			{
				setAuthenticatedContext();
				try
				{
					return authorization.withRestManagement(PROJECT, () ->
					{
						authorization.assertManagerAuthorization(PROJECT);
						scopeEntered.countDown();
						await(releaseScope);
						authorization.assertManagerAuthorization(PROJECT);
						return null;
					});
				} finally
				{
					InvocationContext.setCurrent(null);
				}
			});
			assertThat(scopeEntered.await(10, TimeUnit.SECONDS)).isTrue();

			assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
					.isInstanceOf(AuthorizationException.class);

			releaseScope.countDown();
			scopedOperation.get(10, TimeUnit.SECONDS);
			Throwable reusedThreadFailure = executor.submit(() ->
			{
				setAuthenticatedContext();
				try
				{
					return catchThrowable(() -> authorization.assertManagerAuthorization(PROJECT));
				} finally
				{
					InvocationContext.setCurrent(null);
				}
			}).get(10, TimeUnit.SECONDS);
			assertThat(reusedThreadFailure).isInstanceOf(AuthorizationException.class);
		} finally
		{
			releaseScope.countDown();
			executor.shutdownNow();
			assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		}
	}

	@Test
	void shouldReportGroupLookupFailure()
	{
		when(groupDao.get(PROJECT)).thenThrow(new IllegalStateException("storage unavailable"));

		assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
				.isInstanceOf(InternalException.class).hasMessageContaining(PROJECT);
	}

	@Test
	void shouldReportRoleLookupFailure()
	{
		addGroup(PROJECT, true, true);
		when(attributeDao.getAttributes(PROJECT_MANAGEMENT_AUTHORIZATION_ROLE, CLIENT_ID, PROJECT))
				.thenThrow(new IllegalStateException("storage unavailable"));

		assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
				.isInstanceOf(InternalException.class).hasMessageContaining("entity " + CLIENT_ID);
	}

	private void authorize(AuthorizationCase testCase) throws AuthorizationException
	{
		switch (testCase.check())
		{
		case MANAGER -> authorization.assertManagerAuthorization(testCase.projectPath());
		case MANAGER_WITH_GROUP -> authorization.assertManagerAuthorization(
				testCase.projectPath(), testCase.groupPath());
		case PROJECTS_ADMIN -> authorization.assertProjectsAdminAuthorization(
				testCase.projectPath(), testCase.groupPath());
		case ROLE_MANAGER -> authorization.assertRoleManagerAuthorization(
				testCase.projectPath(), testCase.groupPath(), testCase.requestedRole());
		}
	}

	private void configureGroups(AuthorizationCase testCase)
	{
		addGroup(testCase.projectPath(), true, true);
		if (testCase.check() == AuthorizationCheck.ROLE_MANAGER
				&& !testCase.projectPath().equals(testCase.groupPath()))
			addGroup(testCase.groupPath(), true, true);
	}

	private void addGroup(String path, boolean delegationEnabled, boolean subprojectsEnabled)
	{
		Group group = new Group(path);
		group.setDelegationConfiguration(new GroupDelegationConfiguration(delegationEnabled, subprojectsEnabled,
				null, null, null, null, List.of(), List.of()));
		when(groupDao.get(path)).thenReturn(group);
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

	private static void assertGroupContainmentResult(Throwable failure, AuthorizationCase testCase)
	{
		if (testCase.authorized())
			assertThat(failure).isNull();
		else
			assertThat(failure).isInstanceOf(RuntimeException.class)
					.hasMessage("Group " + testCase.groupPath() + " is not child of main project group " + PROJECT);
	}

	private void configureNestedGroups(String innerProject)
	{
		addGroup(PROJECT, true, true);
		if (!PROJECT.equals(innerProject))
			addGroup(innerProject, true, true);
	}

	private void assertInnerScope(String innerProject) throws AuthorizationException
	{
		authorization.assertManagerAuthorization(innerProject);
		if (!PROJECT.equals(innerProject))
			assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
					.isInstanceOf(AuthorizationException.class);
	}

	private void assertOuterScope(String innerProject) throws AuthorizationException
	{
		authorization.assertManagerAuthorization(PROJECT);
		if (!PROJECT.equals(innerProject))
			assertThatThrownBy(() -> authorization.assertManagerAuthorization(innerProject))
					.isInstanceOf(AuthorizationException.class);
	}

	private void assertNoRestScope(String innerProject)
	{
		assertThatThrownBy(() -> authorization.assertManagerAuthorization(PROJECT))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> authorization.assertManagerAuthorization(innerProject))
				.isInstanceOf(AuthorizationException.class);
	}

	private static Object throwOperationFailure(Exception failure) throws EngineException
	{
		if (failure instanceof EngineException engineException)
			throw engineException;
		throw (RuntimeException) failure;
	}

	private static void await(CountDownLatch latch) throws EngineException
	{
		try
		{
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new EngineException(e);
		}
	}

	private static Stream<AuthorizationCase> authorizationChecks()
	{
		return Arrays.stream(AuthorizationCheck.values())
				.map(check -> AuthorizationCase.builder().withCheck(check).build());
	}

	private static Stream<AuthorizationCase> ordinaryAuthorizationCases()
	{
		return Stream.of(AuthorizationCheck.MANAGER, AuthorizationCheck.MANAGER_WITH_GROUP,
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

	private static Stream<AuthorizationCase> restAuthorizationCases()
	{
		return Stream.concat(authorizationChecks(), Stream.of(regular, projectsAdmin)
				.map(role -> AuthorizationCase.builder().withCheck(AuthorizationCheck.ROLE_MANAGER)
						.withRequestedRole(role).build()));
	}

	private static Stream<AuthorizationCase> differentProjects()
	{
		return Arrays.stream(AuthorizationCheck.values()).flatMap(check ->
				Stream.of("/other", PROJECT + "/sub", PROJECT + "-other").map(path -> AuthorizationCase.builder()
						.withCheck(check).withProjectPath(path).withGroupPath(path + "/sub").build()));
	}

	private static Stream<AuthorizationCase> groupContainmentCases()
	{
		return Stream.of(AuthorizationCheck.MANAGER_WITH_GROUP, AuthorizationCheck.PROJECTS_ADMIN,
				AuthorizationCheck.ROLE_MANAGER).flatMap(check -> Stream.concat(
				Stream.of(PROJECT, PROJECT + "/sub", PROJECT + "/sub/sub2").map(path -> AuthorizationCase.builder()
						.withCheck(check).withGroupPath(path).withAuthorized(true).build()),
				Stream.of("/other", PROJECT + "-other").map(path -> AuthorizationCase.builder()
						.withCheck(check).withGroupPath(path).build())));
	}

	private static Stream<AuthorizationCase> nestedProjects()
	{
		return Stream.of(PROJECT, "/other").map(path -> AuthorizationCase.builder().withProjectPath(path).build());
	}

	private static Stream<ScopeFailureCase> operationFailures()
	{
		return Stream.of(new EngineException("operation failed"), new IllegalStateException("operation failed"))
				.map(failure -> ScopeFailureCase.builder().withProjectPath(PROJECT).withFailure(failure).build());
	}

	private static Stream<ScopeFailureCase> nestedOperationFailures()
	{
		return Stream.of(PROJECT, "/other").flatMap(path -> operationFailures()
				.map(testCase -> ScopeFailureCase.builder().withProjectPath(path)
						.withFailure(testCase.failure()).build()));
	}

	private enum AuthorizationCheck
	{
		MANAGER, MANAGER_WITH_GROUP, PROJECTS_ADMIN, ROLE_MANAGER
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

			private Builder withProjectPath(String projectPath)
			{
				this.projectPath = projectPath;
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

	private record ScopeFailureCase(String projectPath, Exception failure)
	{
		private static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private String projectPath;
			private Exception failure;

			private Builder withProjectPath(String projectPath)
			{
				this.projectPath = projectPath;
				return this;
			}

			private Builder withFailure(Exception failure)
			{
				this.failure = failure;
				return this;
			}

			private ScopeFailureCase build()
			{
				return new ScopeFailureCase(projectPath, failure);
			}
		}
	}
}
