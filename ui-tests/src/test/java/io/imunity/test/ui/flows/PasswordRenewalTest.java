package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;

@Tag("authentication")
class PasswordRenewalTest extends EndUserTestBase
{
	@Autowired
	@Qualifier("insecure")
	private EntityCredentialManagement credentials;

	@Test
	void shouldGateProtectedContentUntilOutdatedPasswordIsReplaced() throws Exception
	{
		TestUser user = createUser("Outdated password owner");
		credentials.setEntityCredentialStatus(user.entity(), "sys:password", LocalCredentialState.outdated);
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/home");
		page.signIn(user.username(), user.password());
		page.visible(By.className("u-outdatedcred-panel"));
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		driver.get(baseUrl + "/home/profile");
		page.visible(By.className("u-outdatedcred-panel"));
		page.click(By.className("u-outdatedcred-update"));
		assertThat(entities.getEntity(user.entity()).getCredentialInfo().getCredentialsState()
				.get("sys:password").getState()).isEqualTo(LocalCredentialState.outdated);

		String replacement = "Renewed!73-Sailboat";
		page.fill(By.className("u-password-setup"), replacement);
		page.fill(By.className("u-password-repeat"), replacement);
		page.click(By.className("u-outdatedcred-update"));
		page.visible(By.className("u-passwordUsernameField"));
		page.signIn(user.username(), replacement);
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(entities.getEntity(user.entity()).getCredentialInfo().getCredentialsState()
				.get("sys:password").getState()).isEqualTo(LocalCredentialState.correct);

		WebDriver oldBrowser = newBrowser();
		VaadinPage oldLogin = new VaadinPage(oldBrowser);
		oldBrowser.get(baseUrl + "/home");
		oldLogin.signIn(user.username(), user.password());
		oldLogin.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(oldBrowser.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		new VaadinPage(newBrowser()).login(baseUrl, "/home", user.username(), replacement);
	}

	@Test
	void shouldRejectLoginWhenRequiredPasswordIsMissing() throws Exception
	{
		TestUser user = createUser("Missing credential owner");
		credentials.setEntityCredentialStatus(user.entity(), "sys:password", LocalCredentialState.notSet);
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/home");
		page.signIn(user.username(), user.password());
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		assertThat(driver.getCurrentUrl()).contains("/authentication");
		assertThat(entities.getEntity(user.entity()).getCredentialInfo().getCredentialsState()
				.get("sys:password").getState()).isEqualTo(LocalCredentialState.notSet);
	}
}
