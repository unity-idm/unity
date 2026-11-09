package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.session.SessionManagementImpl;

@Tag("authentication")
class SessionProtectionTest extends EndUserTestBase
{
	@Autowired
	private SessionManagement sessions;
	@Autowired
	private TokensManagement tokens;

	@Test
	void shouldRequireAuthenticationAfterLogoutIncludingBackAndRefresh() throws Exception
	{
		TestUser user = createUser("Logout owner");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/home", user.username(), user.password());
		driver.get(baseUrl + "/home/profile");
		page.visible(By.id("MainHeader.loggedAs"));
		LoginSession session = sessions.getOwnedSession(user.entity(), "ui");
		page.click(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(tokens.getOwnedTokens(SessionManagementImpl.SESSION_TOKEN_TYPE, user.entity()))
				.noneMatch(token -> token.getValue().equals(session.getId()));

		driver.navigate().back();
		driver.navigate().refresh();
		assertProtected(page);
		driver.get(baseUrl + "/home/profile");
		assertProtected(page);
		page.signIn(user.username(), user.password());
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}

	@Test
	void shouldRequireAuthenticationWhenInactivityLimitHasElapsed() throws Exception
	{
		TestUser user = createUser("Expired session owner");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/home", user.username(), user.password());
		LoginSession session = sessions.getOwnedSession(user.entity(), "ui");
		session.setLastUsed(Date.from(Instant.now().minusSeconds(601)));
		tokens.updateToken(SessionManagementImpl.SESSION_TOKEN_TYPE, session.getId(), null, session.getTokenContents());

		driver.get(baseUrl + "/home/profile");
		assertProtected(page);
		page.signIn(user.username(), user.password());
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(sessions.getOwnedSession(user.entity(), "ui").getId()).isNotEqualTo(session.getId());
	}

	private void assertProtected(VaadinPage page)
	{
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		assertThat(driver.findElements(VaadinPage.testId("ProfileView.save"))).isEmpty();
	}
}
