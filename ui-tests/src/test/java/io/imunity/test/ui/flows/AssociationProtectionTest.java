package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;

@Tag("association")
class AssociationProtectionTest extends SamlWorkflowTestBase
{
	@Test
	void shouldKeepIdentitiesWhenCancellingSecuredAssociationAndRevisitingStatus() throws Exception
	{
		TestUser user = createUser("Secured association owner");
		var original = entities.getEntity(user.entity()).getIdentities();
		deployAt("WellKnownLinksHandler", "/well-known", "");
		driver.get(baseUrl + "/well-known/sec/account-association");
		VaadinPage page = new VaadinPage(driver);
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.findElements(By.id("Wizard.finish"))).isEmpty();
		page.signIn(user.username(), user.password());
		page.visible(By.id("Wizard.next"));
		assertThat(driver.findElements(By.id("Wizard.finish"))).noneMatch(element -> element.isDisplayed());
		page.click(By.id("Wizard.cancel"));
		waitFor(() -> driver.getCurrentUrl().contains("/sec/status"));
		assertThat(page.visible(By.tagName("h3")).getText()).containsIgnoringCase("cancel");
		screenshot("Cancelled association status");
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
		driver.navigate().refresh();
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
		driver.get(baseUrl + "/well-known/sec/status");
		page.visible(By.tagName("body"));
		waitForUi();
		assertThat(driver.findElements(By.id("Wizard.finish"))).isEmpty();
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
		driver.get(baseUrl + "/well-known/sec/account-association?state=stale-workflow");
		page.visible(By.id("Wizard.next"));
		assertThat(driver.findElements(By.id("Wizard.finish"))).noneMatch(element -> element.isDisplayed());
		page.click(By.id("Wizard.cancel"));
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
	}

	@Test
	void shouldRejectAssociationWithIdentityAlreadyAssignedToCurrentAccount() throws Exception
	{
		TestUser user = createUser("Association conflict owner");
		var original = entities.getEntity(user.entity()).getIdentities();
		deploySaml("", List.of());
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/home", user.username(), user.password());
		driver.get(baseUrl + "/saml-home");
		page.visible(By.id("MainHeader.loggedAs"));
		page.click(By.id("EntityDetailsWithActions.associateAccount"));
		String source = driver.getWindowHandle();
		page.click(By.id("Wizard.next"));
		waitForUi();
		if (driver.getWindowHandles().size() == 1)
			page.click(By.xpath("//vaadin-button[normalize-space(.)='Open sandbox window']"));
		waitFor(() -> driver.getWindowHandles().size() > 1);
		driver.switchTo().window(driver.getWindowHandles().stream().filter(handle -> !handle.equals(source))
				.findFirst().orElseThrow());
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		page.click(By.className("u-consentConfirmButton"));
		driver.switchTo().window(source);
		page.visible(By.cssSelector("vaadin-icon[icon='vaadin:exclamation-circle']"));
		waitFor(() -> page.visible(By.id("Wizard.finish")).getDomAttribute("disabled") != null);
		screenshot("Association identity conflict");
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
		page.click(By.id("Wizard.cancel"));
		driver.navigate().refresh();
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
	}
}
