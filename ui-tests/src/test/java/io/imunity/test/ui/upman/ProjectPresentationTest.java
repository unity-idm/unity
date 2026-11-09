package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;
import static io.imunity.vaadin.endpoint.common.LanguageCookie.LANGUAGE_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.manager;
import static pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole.regular;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;

import io.imunity.test.ui.VaadinPage;

@Tag("upman")
class ProjectPresentationTest extends ProjectWorkflowTestBase
{
	@Test
	void shouldFilterProjectMembersWithPolishLocaleInNarrowViewport() throws Exception
	{
		TestUser owner = createUser("Narrow viewport manager");
		TestUser member = createUser("Zażółć gęślą jaźń");
		Project project = createProject("Projekt Selenium", false);
		join(project, owner, manager);
		join(project, member, regular);
		var original = members(project);
		driver.manage().window().setSize(new Dimension(420, 900));
		ProjectPage members = openMembers(owner);
		driver.manage().addCookie(new Cookie(LANGUAGE_COOKIE, "pl"));
		driver.navigate().refresh();
		members.filter(member.displayName());
		waitForElement(ProjectPage.memberActions(member.entity().getEntityId()));
		waitForElementNotPresent(ProjectPage.memberActions(owner.entity().getEntityId()));
		assertThat(new VaadinPage(driver).visible(testId("UpManMembers.grid")).getText())
				.contains(member.displayName());
		members.filter("");
		waitForElement(ProjectPage.memberActions(owner.entity().getEntityId()));
		assertThat(members(project)).isEqualTo(original);
		driver.navigate().refresh();
		waitForElement(ProjectPage.memberActions(member.entity().getEntityId()));
		assertThat(driver.manage().getCookieNamed(LANGUAGE_COOKIE).getValue()).isEqualTo("pl");
		assertRole(owner, project, manager);
	}
}
