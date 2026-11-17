package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.GroupsManagement;

@Tag("admin-directory")
class GroupLifecycleTest extends SeleniumTestBase
{
	private final String parent = "/ui-parent-" + UUID.randomUUID();
	private final String child = parent + "/child";
	private boolean parentCreated;

	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groups;

	@AfterEach
	void removeGroups() throws Exception
	{
		if (parentCreated)
			groups.removeGroup(parent, true);
	}

	@Test
	void shouldCreateEditAndDeleteNestedGroup() throws Exception
	{
		Group preparedParent = new Group(parent);
		preparedParent.setDisplayedName(new I18nString("Parent"));
		groups.addGroup(preparedParent);
		parentCreated = true;
		VaadinPage page = new VaadinPage(driver);
		DirectoryPage directory = new DirectoryPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/directory-browser");
		directory.selectGroup(parent);
		openActions(page, parent, "add");
		page.fill(testId("GroupAddDialog", "GroupAddDialog.name"), "child");
		page.fill(testId("GroupAddDialog", "GroupAddDialog.displayedName"), "Team A");
		page.click(testId("GroupAddDialog", "DialogWithActionFooter.confirm"));
		waitForElementNotPresent(testId("GroupAddDialog"));
		assertThat(groups.getContents(parent, GroupContents.GROUPS).getSubGroups()).containsExactly(child);
		directory.selectGroup(child);
		openActions(page, child, "edit");
		assertThat(page.value(testId("GroupEditDialog", "GroupEditDialog.displayedName")))
				.isEqualTo("Team A");
		page.fill(testId("GroupEditDialog", "GroupEditDialog.displayedName"), "Team B");
		page.click(testId("GroupEditDialog", "DialogWithActionFooter.confirm"));
		waitForElementNotPresent(testId("GroupEditDialog"));

		driver.navigate().refresh();
		directory.selectGroup(child);
		openActions(page, child, "edit");
		assertThat(page.value(testId("GroupEditDialog", "GroupEditDialog.displayedName")))
				.isEqualTo("Team B");
		screenshot("Reloaded group editor");
		assertThat(groups.getContents(child, GroupContents.METADATA).getGroup().getDisplayedName()
				.getValue("en")).isEqualTo("Team B");
		page.click(testId("GroupEditDialog", "DialogWithActionFooter.confirm"));
		waitForElementNotPresent(testId("GroupEditDialog"));
		openActions(page, child, "delete");
		page.click(By.cssSelector("[data-testid='GroupsTreeGrid.deleteConfirmation'] [slot='confirm-button']"));
		waitForElementNotPresent(testId("GroupsTreeGrid.deleteConfirmation"));
		assertThat(groups.getContents(parent, GroupContents.GROUPS).getSubGroups()).isEmpty();
		driver.navigate().refresh();
		directory.selectGroup(parent);
		assertThat(driver.findElements(By.cssSelector("[data-testid='GroupsTreeGrid'] [data-group-path='" + child + "']")))
				.isEmpty();
		assertThat(groups.getContents(parent, GroupContents.METADATA).getGroup().getPathEncoded()).isEqualTo(parent);
	}

	private void openActions(VaadinPage page, String path, String action)
	{
		page.click(By.cssSelector("[data-testid='GroupsTreeGrid.actions'][data-group-path='" + path + "']"));
		page.click(testId("GroupsTreeGrid." + action));
	}
}
