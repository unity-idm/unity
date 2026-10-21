package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;

@Tag("upman")
class ProjectMembershipTest extends UserAccountTestBase
{
	private final List<String> createdProjects = new ArrayList<>();

	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groups;

	@AfterEach
	void removeProjects() throws EngineException
	{
		EngineException failure = null;
		for (String project : createdProjects.reversed())
		{
			try
			{
				groups.removeGroup(project, true);
			} catch (EngineException e)
			{
				if (failure == null)
					failure = e;
				else
					failure.addSuppressed(e);
			}
		}
		if (failure != null)
			throw failure;
	}

	@Test
	void shouldPersistMemberInSelectedSubgroupWithoutChangingUnrelatedProject() throws Exception
	{
		TestUser manager = createUser("Project manager");
		TestUser member = createUser("Project member");
		TestUser control = createUser("Unrelated member");
		String project = createProject("Selenium project");
		String team = project + "/team";
		createSubgroup(team, "Selenium team");
		groups.addMemberFromParent(project, manager.entity());
		groups.addMemberFromParent(project, member.entity());
		attributes.setAttribute(manager.entity(), EnumAttribute.of("sys:ProjectManagementRole", project, "manager"));

		String unrelated = createProject("Unrelated project");
		String unrelatedTeam = unrelated + "/team";
		createSubgroup(unrelatedTeam, "Unrelated team");
		groups.addMemberFromParent(unrelated, member.entity());
		groups.addMemberFromParent(unrelated, control.entity());
		groups.addMemberFromParent(unrelatedTeam, control.entity());
		Set<Long> unrelatedMembers = members(unrelated);
		Set<Long> unrelatedTeamMembers = members(unrelatedTeam);
		assertThat(members(team)).isEmpty();

		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/upman/members", manager.username(), manager.password());
		page.select(testId("UpManMembers.group"), "Selenium project (All members)");
		page.click(memberActions(member.entity().getEntityId()));
		page.click(testId("UpManMembers.addToGroup"));
		page.select(testId("UpManMembers.addToGroupDialog", "UpManMembers.targetGroup"), "Selenium team");
		page.click(testId("UpManMembers.addToGroupDialog", "UpManMembers.confirmAdd"));
		waitForElementNotPresent(testId("UpManMembers.confirmAdd"));

		driver.navigate().refresh();
		page.select(testId("UpManMembers.group"), "Selenium team");
		page.visible(memberActions(member.entity().getEntityId()));
		assertThat(page.visible(testId("UpManMembers.grid")).getText()).contains(member.displayName());
		assertThat(members(team)).containsExactly(member.entity().getEntityId());
		assertThat(entities.getGroups(member.entity())).containsKeys(project, team, unrelated);
		assertThat(members(unrelated)).isEqualTo(unrelatedMembers);
		assertThat(members(unrelatedTeam)).isEqualTo(unrelatedTeamMembers);
	}

	private String createProject(String displayName) throws EngineException
	{
		String path = "/ui-project-" + UUID.randomUUID();
		Group project = new Group(path);
		project.setDisplayedName(new I18nString(displayName));
		project.setDelegationConfiguration(new GroupDelegationConfiguration(
				true, false, null, null, null, null, List.of(), List.of()));
		groups.addGroup(project);
		createdProjects.add(path);
		return path;
	}

	private void createSubgroup(String path, String displayName) throws EngineException
	{
		Group subgroup = new Group(path);
		subgroup.setDisplayedName(new I18nString(displayName));
		groups.addGroup(subgroup);
	}

	private Set<Long> members(String path) throws EngineException
	{
		return groups.getContents(path, GroupContents.MEMBERS).getMembers().stream()
				.map(GroupMembership::getEntityId).collect(Collectors.toSet());
	}

	private By memberActions(long entityId)
	{
		return By.cssSelector("[data-testid='UpManMembers.grid'] "
				+ "[data-testid='UpManMembers.memberActions'][data-member-id='" + entityId + "']");
	}
}
