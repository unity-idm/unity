package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.manager;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.regular;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.group.Group;

@Tag("upman")
class ProjectScopeTest extends ProjectWorkflowTestBase
{
	@ParameterizedTest
	@MethodSource("accessCases")
	void shouldKeepProtectedProjectDataAndActionsOutsideUnprivilegedUserScope(AccessCase scenario) throws Exception
	{
		TestUser owner = createUser("Protected project manager");
		TestUser protectedMember = createUser("Confidential project member");
		TestUser actor = createUser("Scope boundary actor");
		Project target = createProject("Confidential project", true);
		Project child = createChild(target, "Confidential team", false);
		Project unrelated = createProject("Actor project", false);
		join(target, owner, manager);
		join(target, protectedMember, regular);
		join(child, protectedMember, regular);
		if (scenario.actor() == Actor.REGULAR_MEMBER)
			join(target, actor, regular);
		if (scenario.actor() == Actor.OTHER_PROJECT_MANAGER)
			join(unrelated, actor, manager);
		Set<Long> targetSnapshot = members(target);
		Set<Long> childSnapshot = members(child);
		Group groupSnapshot = storedGroup(target);
		VaadinPage page = new VaadinPage(driver);
		if (scenario.actor() == Actor.OTHER_PROJECT_MANAGER)
			openMembers(actor);
		else
			assertNoProject(driver, actor);

		for (String route : List.of("members", "groups", "invitations", "user-updates"))
		{
			driver.get(baseUrl + "/upman/" + route);
			if (scenario.actor() == Actor.OTHER_PROJECT_MANAGER)
			{
				page.visible(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
				waitFor(() -> driver.findElement(By.tagName("body")).getText().contains(unrelated.label()));
			}
			else
				waitFor(() -> driver.findElement(By.tagName("body")).getText().contains(NO_PROJECT));
			assertThat(driver.findElement(By.tagName("body")).getText())
					.doesNotContain(target.label(), child.label(), protectedMember.displayName());
			assertThat(driver.findElements(ProjectPage.memberActions(protectedMember.entity().getEntityId())))
					.isEmpty();
			assertThat(driver.findElements(By.cssSelector("[data-group-path^='" + target.path() + "']")))
					.isEmpty();
			assertThat(driver.findElements(testId("UpManMenu.project"))).isEmpty();
		}
		assertThat(members(target)).isEqualTo(targetSnapshot);
		assertThat(members(child)).isEqualTo(childSnapshot);
		assertThat(storedGroup(target)).isEqualTo(groupSnapshot);
		assertRole(owner, target, manager);
		screenshot("Project permission boundary");
	}

	private static Stream<AccessCase> accessCases()
	{
		return Stream.of(AccessCase.builder().actor(Actor.REGULAR_MEMBER).build(),
				AccessCase.builder().actor(Actor.UNRELATED_USER).build(),
				AccessCase.builder().actor(Actor.OTHER_PROJECT_MANAGER).build());
	}

	enum Actor
	{
		REGULAR_MEMBER, UNRELATED_USER, OTHER_PROJECT_MANAGER
	}

	record AccessCase(Actor actor)
	{
		static Builder builder()
		{
			return new Builder();
		}

		static final class Builder
		{
			private Actor actor;

			Builder actor(Actor actor)
			{
				this.actor = actor;
				return this;
			}

			AccessCase build()
			{
				return new AccessCase(actor);
			}
		}
	}
}
