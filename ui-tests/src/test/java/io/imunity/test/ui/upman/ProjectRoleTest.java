package io.imunity.test.ui.upman;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.manager;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.projectsAdmin;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.regular;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.WebDriver;

import io.imunity.test.ui.VaadinPage;

@Tag("upman")
class ProjectRoleTest extends ProjectWorkflowTestBase
{
	@ParameterizedTest
	@MethodSource("roleScopes")
	void shouldGrantAndRevokeManagementRoleWithFreshSessionPermissionsFollowingScope(RoleScope scenario)
			throws Exception
	{
		TestUser admin = createUser("Role administrator");
		TestUser target = createUser("Role recipient");
		TestUser control = createUser("Unrelated manager");
		Project project = createProject("Role project", true);
		Project child = createChild(project, "Role subproject", true);
		Project unrelated = createProject("Role control", false);
		join(project, admin, projectsAdmin);
		join(project, target, regular);
		join(child, target, regular);
		join(unrelated, control, manager);
		Set<Long> rootSnapshot = members(project);
		Set<Long> childSnapshot = members(child);
		Set<Long> controlSnapshot = members(unrelated);
		Project scope = scenario.subproject() ? child : project;
		ProjectPage page = openMembers(admin);
		if (scenario.subproject())
			page.selectGroup(child.subproject());
		page.setRole(target.entity().getEntityId(), scenario.subproject(), "manager");
		assertRole(target, scope, manager);

		screenshot("Granted project role");
		WebDriver granted = newBrowser();
		openMembers(granted, target);
		granted.get(baseUrl + "/upman/groups");
		assertThat(new VaadinPage(granted).visible(ProjectPage.groupRow(scope.path())).getText())
				.contains(scope.label());
		if (scenario.subproject())
		{
			assertThat(granted.findElements(ProjectPage.groupRow(project.path()))).isEmpty();
			assertRole(target, project, regular);
		}

		driver.navigate().refresh();
		if (scenario.subproject())
			page.selectGroup(child.subproject());
		page.setRole(target.entity().getEntityId(), scenario.subproject(), "regular");
		assertRole(target, scope, regular);
		assertNoProject(newBrowser(), target);
		new VaadinPage(newBrowser()).login(baseUrl, "/home", target.username(), target.password());
		assertRole(admin, project, projectsAdmin);
		assertRole(control, unrelated, manager);
		assertThat(members(project)).isEqualTo(rootSnapshot);
		assertThat(members(child)).isEqualTo(childSnapshot);
		assertThat(members(unrelated)).isEqualTo(controlSnapshot);
	}

	private static Stream<RoleScope> roleScopes()
	{
		return Stream.of(RoleScope.builder().subproject(false).build(), RoleScope.builder().subproject(true).build());
	}

	record RoleScope(boolean subproject)
	{
		static Builder builder()
		{
			return new Builder();
		}

		static final class Builder
		{
			private boolean subproject;

			Builder subproject(boolean subproject)
			{
				this.subproject = subproject;
				return this;
			}

			RoleScope build()
			{
				return new RoleScope(subproject);
			}
		}
	}
}
