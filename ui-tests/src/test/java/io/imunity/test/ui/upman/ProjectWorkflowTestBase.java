package io.imunity.test.ui.upman;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;

abstract class ProjectWorkflowTestBase extends UserAccountTestBase
{
	static final String ROLE_ATTRIBUTE = "sys:ProjectManagementRole";
	static final String NO_PROJECT = "You are not authorized to manage any projects.";
	private final List<String> projects = new ArrayList<>();

	@Autowired
	@Qualifier("insecure")
	GroupsManagement groups;

	@AfterEach
	void removeProjects() throws EngineException
	{
		EngineException failure = null;
		for (String project : projects.reversed())
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

	final Project createProject(String label, boolean allowSubprojects) throws Exception
	{
		String path = "/ui-end-user-project-" + UUID.randomUUID();
		Group project = new Group(path);
		project.setDisplayedName(new I18nString(label));
		project.setDelegationConfiguration(new GroupDelegationConfiguration(
				true, allowSubprojects, null, null, null, null, List.of(), List.of()));
		groups.addGroup(project);
		projects.add(path);
		return new Project(path, label);
	}

	final Project createChild(Project parent, String label, boolean delegated) throws Exception
	{
		String path = parent.path() + "/team-" + UUID.randomUUID();
		Group group = new Group(path);
		group.setDisplayedName(new I18nString(label));
		group.setDelegationConfiguration(new GroupDelegationConfiguration(delegated));
		groups.addGroup(group);
		return new Project(path, label);
	}

	final void join(Project project, TestUser user, GroupAuthorizationRole role) throws Exception
	{
		groups.addMemberFromParent(project.path(), user.entity());
		attributes.setAttribute(user.entity(), EnumAttribute.of(ROLE_ATTRIBUTE, project.path(), role.toString()));
	}

	final Set<Long> members(Project project) throws Exception
	{
		return groups.getContents(project.path(), GroupContents.MEMBERS).getMembers().stream()
				.map(GroupMembership::getEntityId).collect(Collectors.toSet());
	}

	final Group storedGroup(Project project) throws Exception
	{
		return groups.getContents(project.path(), GroupContents.METADATA).getGroup();
	}

	final void assertRole(TestUser user, Project project, GroupAuthorizationRole expected) throws Exception
	{
		assertThat(attributes.getAttributes(user.entity(), project.path(), ROLE_ATTRIBUTE)).singleElement()
				.satisfies(attribute -> assertThat(attribute.getValues()).containsExactly(expected.toString()));
	}

	final ProjectPage openMembers(TestUser user)
	{
		return openMembers(driver, user);
	}

	final ProjectPage openMembers(WebDriver browser, TestUser user)
	{
		new VaadinPage(browser).login(baseUrl, "/upman/members", user.username(), user.password());
		ProjectPage page = new ProjectPage(browser);
		page.grid();
		return page;
	}

	final void assertNoProject(WebDriver browser, TestUser user)
	{
		browser.get(baseUrl + "/upman/members");
		new VaadinPage(browser).signIn(user.username(), user.password());
		waitFor(() -> browser.findElement(By.tagName("body")).getText().contains(NO_PROJECT));
		assertThat(browser.findElements(ProjectPage.memberActions(user.entity().getEntityId()))).isEmpty();
	}

	record Project(String path, String label)
	{
		String allMembers()
		{
			return label + " (All members)";
		}

		String subproject()
		{
			return label + " (Sub-project)";
		}
	}
}
