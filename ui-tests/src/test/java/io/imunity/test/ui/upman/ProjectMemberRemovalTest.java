package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.manager;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.regular;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.entity.EntityState;

@Tag("upman")
class ProjectMemberRemovalTest extends ProjectWorkflowTestBase
{
	@ParameterizedTest
	@MethodSource("removalScopes")
	void shouldCancelSelfRemovalAndRemoveOnlySelectedMemberFromIntendedScope(RemovalScope scenario) throws Exception
	{
		TestUser owner = createUser("Removal manager");
		TestUser member = createUser("Removed project member");
		TestUser control = createUser("Preserved project member");
		Project project = createProject("Membership removal project", false);
		Project child = createChild(project, "Membership removal team", false);
		Project unrelated = createProject("Membership control project", false);
		join(project, owner, manager);
		join(project, member, regular);
		join(project, control, regular);
		join(child, owner, regular);
		join(child, member, regular);
		join(child, control, regular);
		join(unrelated, member, regular);
		join(unrelated, control, regular);
		Set<Long> rootSnapshot = members(project);
		Set<Long> childSnapshot = members(child);
		Set<Long> unrelatedSnapshot = members(unrelated);
		Project scope = scenario.project() ? project : child;
		ProjectPage page = openMembers(owner);
		if (!scenario.project())
			page.selectGroup(child.label());
		String action = scenario.project() ? "removeFromProject" : "removeFromGroup";
		page.action(owner.entity().getEntityId(), action);
		assertThat(new VaadinPage(driver).visible(testId("UpManMembers.selfRemoveDialog")).getText())
				.contains(scope.label());
		page.cancel("UpManMembers.selfRemoveDialog");
		assertThat(members(project)).isEqualTo(rootSnapshot);
		assertThat(members(child)).isEqualTo(childSnapshot);
		openMembers(newBrowser(), owner);

		page.action(member.entity().getEntityId(), action);
		waitForElementNotPresent(ProjectPage.memberActions(member.entity().getEntityId()));
		driver.navigate().refresh();
		if (!scenario.project())
			page.selectGroup(child.label());
		waitForElement(ProjectPage.memberActions(control.entity().getEntityId()));
		assertThat(driver.findElements(ProjectPage.memberActions(member.entity().getEntityId()))).isEmpty();
		assertThat(members(scope)).containsExactlyInAnyOrder(owner.entity().getEntityId(), control.entity().getEntityId());
		assertThat(members(child)).doesNotContain(member.entity().getEntityId());
		if (!scenario.project())
			assertThat(members(project)).isEqualTo(rootSnapshot);
		assertThat(members(unrelated)).isEqualTo(unrelatedSnapshot);
		assertThat(entities.getGroups(member.entity())).containsKey(unrelated.path()).doesNotContainKey(scope.path());
		assertThat(entities.getEntity(member.entity()).getState()).isEqualTo(EntityState.valid);
		new VaadinPage(newBrowser()).login(baseUrl, "/home", member.username(), member.password());
		assertRole(owner, project, manager);
	}

	private static Stream<RemovalScope> removalScopes()
	{
		return Stream.of(RemovalScope.builder().project(true).build(), RemovalScope.builder().project(false).build());
	}

	record RemovalScope(boolean project)
	{
		static Builder builder()
		{
			return new Builder();
		}

		static final class Builder
		{
			private boolean project;

			Builder project(boolean project)
			{
				this.project = project;
				return this;
			}

			RemovalScope build()
			{
				return new RemovalScope(project);
			}
		}
	}
}
