package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.manager;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.projectsAdmin;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.regular;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;

@Tag("upman")
class ProjectBrowsingTest extends ProjectWorkflowTestBase
{
	@Test
	void shouldSwitchProjectsAndFilterMembersWithActionsFollowingSelectedProjectRole() throws Exception
	{
		TestUser owner = createUser("Switching manager");
		TestUser firstMember = createUser("First project member");
		TestUser secondMember = createUser("Second project member");
		Project first = createProject("First browsing project", false);
		Project second = createProject("Second browsing project", true);
		join(first, owner, manager);
		join(first, firstMember, regular);
		join(second, owner, projectsAdmin);
		join(second, secondMember, regular);
		Set<Long> firstSnapshot = members(first);
		Set<Long> secondSnapshot = members(second);
		ProjectPage project = openMembers(owner);
		project.selectProject(first.label());
		assertMembers(firstMember, secondMember);
		assertRoleChoices(project, owner, "manager", "regular");

		project.filter(firstMember.displayName());
		waitFor(() -> driver.findElements(ProjectPage.memberActions(owner.entity().getEntityId())).isEmpty());
		assertMembers(firstMember, owner);
		project.filter("no-such-project-member");
		waitFor(() -> driver.findElements(testId("UpManMembers.grid", "UpManMembers.memberActions")).isEmpty());
		project.filter("");
		waitForElement(ProjectPage.memberActions(owner.entity().getEntityId()));
		assertMembers(firstMember, secondMember);

		project.selectProject(second.label());
		assertMembers(secondMember, firstMember);
		assertRoleChoices(project, owner, "projects admin", "manager", "regular");
		driver.navigate().refresh();
		project.selectProject(second.label());
		assertMembers(secondMember, firstMember);
		project.selectProject(first.label());
		assertMembers(firstMember, secondMember);
		assertThat(members(first)).isEqualTo(firstSnapshot);
		assertThat(members(second)).isEqualTo(secondSnapshot);
		assertRole(owner, first, manager);
		assertRole(owner, second, projectsAdmin);
		screenshot("Project members");
	}

	private void assertMembers(TestUser present, TestUser absent)
	{
		waitForElement(ProjectPage.memberActions(present.entity().getEntityId()));
		waitForElementNotPresent(ProjectPage.memberActions(absent.entity().getEntityId()));
		assertThat(new VaadinPage(driver).visible(testId("UpManMembers.grid")).getText())
				.contains(present.displayName()).doesNotContain(absent.displayName());
	}

	private void assertRoleChoices(ProjectPage project, TestUser user, String... roles)
	{
		project.action(user.entity().getEntityId(), "setProjectRole");
		assertThat(new VaadinPage(driver).visible(testId("UpManMembers.projectRoleDialog", "UpManMembers.role"))
				.findElements(By.tagName("vaadin-radio-button"))).extracting(option -> option.getText())
				.containsExactly(roles);
		project.cancel("UpManMembers.projectRoleDialog");
	}
}
