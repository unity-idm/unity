package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.imunity.test.ui.VaadinPage;

@Tag("authentication")
class LoginLockoutTest extends EndUserTestBase
{
	@Test
	void shouldBlockAfterRepeatedFailuresAndAllowLoginAfterExpiry() throws Exception
	{
		TestUser user = createUser("Lockout owner");
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/home");
		for (int attempt = 0; attempt < 4; attempt++)
		{
			page.signIn(user.username(), "Incorrect!69-Password");
			page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
			waitForElementNotPresent(By.cssSelector("vaadin-notification-card[theme~='error']"));
		}
		page.signIn(user.username(), "Incorrect!69-Password");
		WebElement blocked = page.visible(By.cssSelector("vaadin-dialog[opened]"));
		assertThat(blocked.getText()).containsIgnoringCase("blocked");
		screenshot("Login lockout countdown");
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		waitFor(() -> driver.findElements(By.cssSelector("vaadin-dialog[opened]")).stream()
				.noneMatch(WebElement::isDisplayed));
		page.signIn(user.username(), user.password());
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}
}
