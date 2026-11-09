package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.EndUserTestBase;

@Tag("admin-services")
class HomeServiceEditorTest extends EndUserTestBase
{
	@Test
	void shouldPersistHomeGeneralAuthenticationAndUserControlsAndApplyThemToLogin() throws Exception
	{
		TestUser user = createUser("Edited Home service owner");
		String name = deployHomeAt("/edited-home", "");
		String editUrl = baseUrl + "/admin/services/edit/" + URLEncoder.encode(name, StandardCharsets.UTF_8)
				.replace("+", "%20");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(editUrl);
		page.fill(testId("ServiceGeneralTab.description"), "Home settings saved through AdminUI");
		page.check(testId("HomeServiceEditorGeneralTab.mfaOptIn"), false);
		page.click(By.xpath("//vaadin-tab[@orientation='horizontal'][normalize-space(.)='Authentication']"));
		page.fill(testId("WebServiceAuthenticationTab.title"), "Selenium account sign-in");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/services");
		assertThat(endpoints.getEndpoint(name).getConfiguration().getDescription())
				.isEqualTo("Home settings saved through AdminUI");

		driver.get(editUrl);
		assertThat(page.value(testId("ServiceGeneralTab.description")))
				.isEqualTo("Home settings saved through AdminUI");
		assertThat(page.visible(testId("HomeServiceEditorGeneralTab.mfaOptIn")).getDomProperty("checked"))
				.isEqualTo("false");
		page.click(By.xpath("//vaadin-tab[@orientation='horizontal'][normalize-space(.)='Authentication']"));
		assertThat(page.value(testId("WebServiceAuthenticationTab.title"))).isEqualTo("Selenium account sign-in");

		var fresh = newBrowser();
		VaadinPage login = new VaadinPage(fresh);
		fresh.get(baseUrl + "/edited-home");
		login.visible(By.className("u-passwordUsernameField"));
		assertThat(fresh.findElement(By.tagName("body")).getText()).contains("Selenium account sign-in");
		login.signIn(user.username(), user.password());
		assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		fresh.get(baseUrl + "/edited-home/sign-in");
		login.visible(By.cssSelector("[data-testid='HomeSignIn.credential']"));
		assertThat(fresh.findElements(testId("HomeSignIn.mfaOptIn"))).isEmpty();
		fresh.navigate().refresh();
		login.visible(By.cssSelector("[data-testid='HomeSignIn.credential']"));
		assertThat(fresh.findElements(testId("HomeSignIn.mfaOptIn"))).isEmpty();
	}
}
