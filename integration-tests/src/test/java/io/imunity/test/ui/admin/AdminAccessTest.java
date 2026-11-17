package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;

@Tag("admin-directory")
class AdminAccessTest extends UserAccountTestBase
{
	@Test
	void shouldDenyOrdinaryUserDirectAccessToDirectory() throws Exception
	{
		TestUser user = createUser("Ordinary member");
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/admin/directory-browser");

		page.signIn(user.username(), user.password());

		waitFor(() -> driver.findElement(By.tagName("body")).getText()
				.contains("Access denied, you do not have privileges to use admin console"));
		assertThat(driver.findElements(By.cssSelector("[data-group-path]"))).isEmpty();
		assertThat(driver.findElements(testId("IdentitiesPanel.createEntity")))
				.noneMatch(element -> element.isDisplayed() && element.isEnabled());
		assertThat(driver.findElement(By.tagName("body")).getText()).doesNotContain("ui-admin");
		screenshot("Admin access denied");
	}
}
