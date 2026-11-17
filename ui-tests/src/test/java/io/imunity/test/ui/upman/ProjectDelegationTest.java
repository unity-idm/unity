package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.manager;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.projectsAdmin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;

@Tag("upman")
class ProjectDelegationTest extends ProjectWorkflowTestBase
{
	@Test
	void shouldPersistSubprojectDelegationAndPreserveUnrelatedProject() throws Exception
	{
		TestUser admin = createUser("Subproject administrator");
		Project project = createProject("Delegation project", true);
		Project child = createChild(project, "Delegation team", false);
		Project control = createProject("Delegation control", false);
		join(project, admin, projectsAdmin);
		Group controlSnapshot = storedGroup(control);
		ProjectPage projectPage = openMembers(admin);
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/upman/groups");
		projectPage.groupAction(child.path(), "delegate");
		page.check(field("delegation"), true);
		page.check(field("subprojects"), true);
		page.fill(field("logoUrl"), baseUrl + "/project-logo.png");
		page.click(testId("UpManGroups.delegationDialog", "UpManGroups.confirm"));
		waitForElementNotPresent(testId("UpManGroups.delegationDialog"));

		GroupDelegationConfiguration saved = storedGroup(child).getDelegationConfiguration();
		assertThat(saved.enabled).isTrue();
		assertThat(saved.enableSubprojects).isTrue();
		assertThat(saved.logoUrl).isEqualTo(baseUrl + "/project-logo.png");
		driver.navigate().refresh();
		projectPage.groupAction(child.path(), "delegate");
		assertThat(page.visible(field("delegation")).getDomProperty("checked")).isEqualTo("true");
		assertThat(page.visible(field("subprojects")).getDomProperty("checked")).isEqualTo("true");
		assertThat(page.value(field("logoUrl"))).isEqualTo(saved.logoUrl);
		screenshot("Reloaded delegation settings");
		page.check(field("delegation"), false);
		assertThat(page.visible(field("logoUrl")).getDomProperty("disabled")).isEqualTo("true");
		assertThat(page.visible(field("subprojects")).getDomProperty("disabled")).isEqualTo("true");
		page.click(testId("UpManGroups.delegationDialog", "UpManGroups.confirm"));
		waitForElementNotPresent(testId("UpManGroups.delegationDialog"));
		driver.navigate().refresh();
		projectPage.groupAction(child.path(), "delegate");
		assertThat(page.visible(field("delegation")).getDomProperty("checked")).isEqualTo("false");
		projectPage.cancel("UpManGroups.delegationDialog");
		assertThat(storedGroup(child).getDelegationConfiguration().enabled).isFalse();
		assertThat(storedGroup(control)).isEqualTo(controlSnapshot);
		assertRole(admin, project, projectsAdmin);
	}

	@Test
	void shouldKeepDelegationConfigurationUnavailableToProjectManager() throws Exception
	{
		TestUser owner = createUser("Project manager");
		Project project = createProject("Restricted delegation project", true);
		Project child = createChild(project, "Restricted delegation team", false);
		join(project, owner, manager);
		Group original = storedGroup(child);
		openMembers(owner);
		driver.get(baseUrl + "/upman/groups");
		VaadinPage page = new VaadinPage(driver);
		page.click(By.cssSelector("[data-testid='UpManGroups.actions'][data-group-path='" + child.path() + "']"));
		page.visible(testId("UpManGroups.rename"));
		assertThat(driver.findElements(testId("UpManGroups.delegate"))).noneMatch(element -> element.isDisplayed());
		assertThat(storedGroup(child)).isEqualTo(original);
		screenshot("Restricted group actions");
	}

	private By field(String name)
	{
		return testId("UpManGroups.delegationDialog", "SubProjectConfigurationLayout." + name);
	}
}
