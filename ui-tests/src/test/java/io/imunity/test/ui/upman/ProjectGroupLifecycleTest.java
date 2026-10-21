package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;

@Tag("upman")
class ProjectGroupLifecycleTest extends UserAccountTestBase
{
	private final List<String> projects = new ArrayList<>();
	private Group originalRoot;

	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groups;

	@AfterEach
	void removeProjects() throws Exception
	{
		try
		{
			for (String project : projects.reversed())
				groups.removeGroup(project, true);
		} finally
		{
			if (originalRoot != null)
				groups.updateGroup("/", originalRoot);
		}
	}

	@Test
	void shouldPersistGroupNameAndVisibilityWithoutChangingUnrelatedProject() throws Exception
	{
		TestUser manager = createUser("Group manager");
		originalRoot = groups.getContents("/", GroupContents.METADATA).getGroup();
		Group publicRoot = originalRoot.clone();
		publicRoot.setPublic(true);
		groups.updateGroup("/", publicRoot);
		String project = createProject("Selenium lifecycle project");
		String unrelated = createProject("Unrelated project");
		groups.addMemberFromParent(project, manager.entity());
		attributes.setAttribute(manager.entity(), EnumAttribute.of("sys:ProjectManagementRole", project, "manager"));
		GroupContents control = groups.getContents(unrelated, GroupContents.EVERYTHING);
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/upman", manager.username(), manager.password());
		driver.get(baseUrl + "/upman/groups");
		openActions(page, project, "add");
		page.fill(testId("UpManGroups.addDialog", "UpManGroups.displayedName"), "New lifecycle team");
		page.click(testId("UpManGroups.addDialog", "UpManGroups.confirm"));
		waitForElementNotPresent(testId("UpManGroups.addDialog"));
		assertThat(groups.getContents(project, GroupContents.GROUPS).getSubGroups()).hasSize(1);
		String child = groups.getContents(project, GroupContents.GROUPS).getSubGroups().getFirst();
		assertGroup(page, child, "New lifecycle team", true);

		openActions(page, child, "rename");
		page.fill(testId("UpManGroups.renameDialog", "UpManGroups.displayedName"), "Renamed lifecycle team");
		page.click(testId("UpManGroups.renameDialog", "UpManGroups.confirm"));
		waitForElementNotPresent(testId("UpManGroups.renameDialog"));
		driver.navigate().refresh();
		assertGroup(page, child, "Renamed lifecycle team", true);
		openActions(page, child, "makePrivate");
		waitFor(() -> page.visible(groupRow(child))
				.findElements(By.cssSelector("vaadin-icon[icon='vaadin:unlock']")).isEmpty());
		driver.navigate().refresh();
		assertGroup(page, child, "Renamed lifecycle team", false);
		openActions(page, child, "makePublic");
		page.visible(By.cssSelector("[data-testid='UpManGroups.group'][data-group-path='" + child
				+ "'] vaadin-icon[icon='vaadin:unlock']"));
		driver.navigate().refresh();
		assertGroup(page, child, "Renamed lifecycle team", true);

		openActions(page, child, "delete");
		page.click(testId("UpManGroups.deleteConfirmation", "UpManGroups.confirm"));
		waitForElementNotPresent(testId("UpManGroups.deleteConfirmation"));
		assertThat(groups.getContents(project, GroupContents.GROUPS).getSubGroups()).isEmpty();
		driver.navigate().refresh();
		page.visible(groupRow(project));
		assertThat(driver.findElements(groupRow(child))).isEmpty();
		assertThat(groups.getContents(unrelated, GroupContents.EVERYTHING)).isEqualTo(control);
	}

	private String createProject(String name) throws Exception
	{
		String path = "/ui-project-" + UUID.randomUUID();
		Group group = new Group(path);
		group.setDisplayedName(new I18nString(name));
		group.setPublic(true);
		group.setDelegationConfiguration(new GroupDelegationConfiguration(
				true, false, null, null, null, null, List.of(), List.of()));
		groups.addGroup(group);
		projects.add(path);
		return path;
	}

	private void assertGroup(VaadinPage page, String path, String name, boolean isPublic) throws Exception
	{
		waitFor(() -> page.visible(groupRow(path)).getText().equals(name));
		assertThat(page.visible(groupRow(path)).findElements(By.cssSelector("vaadin-icon[icon='vaadin:unlock']")))
				.hasSize(isPublic ? 1 : 0);
		Group stored = groups.getContents(path, GroupContents.METADATA).getGroup();
		assertThat(stored.getDisplayedName().getValue("en")).isEqualTo(name);
		assertThat(stored.isPublic()).isEqualTo(isPublic);
	}

	private void openActions(VaadinPage page, String path, String action)
	{
		page.click(By.cssSelector("[data-testid='UpManGroups.actions'][data-group-path='" + path + "']"));
		page.click(testId("UpManGroups." + action));
	}

	private By groupRow(String path)
	{
		return By.cssSelector("[data-testid='UpManGroups.group'][data-group-path='" + path + "']");
	}
}
