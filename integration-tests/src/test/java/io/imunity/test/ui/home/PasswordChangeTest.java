package io.imunity.test.ui.home;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;

@Tag("home")
class PasswordChangeTest extends UserAccountTestBase
{
	private static final String PASSWORD_PANEL =
			"[data-testid='HomeSignIn.credential'][data-credential='sys:password'] ";

	@Test
	void shouldRejectOldPasswordAndAcceptNewPasswordAfterHomeChange() throws Exception
	{
		TestUser user = createUser("Password change member");
		String newPassword = "Changed!84-Orchard";
		VaadinPage home = new VaadinPage(driver);
		home.login(baseUrl, "/home/sign-in", user.username(), user.password());

		home.click(By.cssSelector(PASSWORD_PANEL + "[data-testid='HomeSignIn.editCredential']"));
		home.fill(By.cssSelector(PASSWORD_PANEL + ".u-password-setup"), newPassword);
		home.fill(By.cssSelector(PASSWORD_PANEL + ".u-password-repeat"), newPassword);
		home.click(By.cssSelector(PASSWORD_PANEL + "[data-testid='HomeSignIn.saveCredential']"));
		waitForElementNotPresent(By.cssSelector(PASSWORD_PANEL + "[data-testid='HomeSignIn.saveCredential']"));

		WebDriver rejectedBrowser = newBrowser();
		VaadinPage rejectedLogin = new VaadinPage(rejectedBrowser);
		rejectedBrowser.get(baseUrl + "/home");
		rejectedLogin.signIn(user.username(), user.password());
		assertThat(rejectedLogin.visible(By.cssSelector("vaadin-notification-card[theme~='error']")).getText())
				.contains("Invalid user name, credential or external authentication failed.");
		assertThat(rejectedBrowser.getCurrentUrl()).contains("/home/authentication");
		assertThat(rejectedBrowser.findElements(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"))).isEmpty();

		VaadinPage freshLogin = new VaadinPage(newBrowser());
		freshLogin.login(baseUrl, "/home", user.username(), newPassword);
		assertThat(freshLogin.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}
}
