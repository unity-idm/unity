package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;

@Tag("admin-directory")
class AccountAdministrationTest extends UserAccountTestBase
{
	@Test
	void shouldDisableAndReactivateAccountAndEnforceStateOnFreshLogin() throws Exception
	{
		TestUser user = createUser("State target");
		TestUser control = createUser("State control");
		AccountAdministrationPage admin = new AccountAdministrationPage(driver);
		admin.open(baseUrl);
		admin.changeState(user.entity(), "DISABLED");
		waitForElementNotPresent(testId("ChangeEntityStateDialog"));
		assertThat(entities.getEntity(user.entity()).getState()).isEqualTo(EntityState.disabled);
		assertRejected(user, user.password());
		assertAccepted(control, control.password());

		driver.navigate().refresh();
		new DirectoryPage(driver).selectGroup("/");
		admin.changeState(user.entity(), "ENABLED");
		waitForElementNotPresent(testId("ChangeEntityStateDialog"));
		assertThat(entities.getEntity(user.entity()).getState()).isEqualTo(EntityState.valid);
		assertAccepted(user, user.password());
	}

	@Test
	void shouldReplaceAdministrativePasswordAndRejectPreviousPassword() throws Exception
	{
		TestUser user = createUser("Password target");
		String updated = "Changed!84-Selenium";
		AccountAdministrationPage admin = new AccountAdministrationPage(driver);
		admin.open(baseUrl);
		admin.action(user.entity(), "Update credential");
		VaadinPage page = new VaadinPage(driver);
		String panel = "[data-testid='CredentialsChangeDialog'] "
				+ "[data-testid='SingleCredentialPanel'][data-credential='sys:password'] ";
		page.click(By.cssSelector(panel + "[data-testid='SingleCredentialPanel.edit']"));
		page.fill(By.cssSelector(panel + ".u-password-setup"), updated);
		page.fill(By.cssSelector(panel + ".u-password-repeat"), updated);
		page.click(By.cssSelector(panel + "[data-testid='SingleCredentialEditComponent.update']"));
		page.visible(By.cssSelector("vaadin-notification-card[theme~='success']"));
		page.click(testId("CredentialsChangeDialog", "DialogWithActionFooter.confirm"));
		waitForElementNotPresent(testId("CredentialsChangeDialog"));
		assertRejected(user, user.password());
		assertAccepted(user, updated);
	}

	@Test
	void shouldPreserveAccountOnCancellationAndRemoveItOnConfirmation() throws Exception
	{
		TestUser user = createUser("Delete target");
		TestUser control = createUser("Delete control");
		AccountAdministrationPage admin = new AccountAdministrationPage(driver);
		admin.open(baseUrl);
		admin.action(user.entity(), "Remove entity");
		admin.confirmDeletion(false);
		assertThat(entities.getEntity(user.entity()).getState()).isEqualTo(EntityState.valid);
		assertAccepted(user, user.password());

		admin.action(user.entity(), "Remove entity");
		admin.confirmDeletion(true);
		waitForElementNotPresent(By.cssSelector("[data-testid='IdentitiesTreeGrid.entityActions'][data-entity-id='"
				+ user.entity().getEntityId() + "']"));
		assertThatThrownBy(() -> entities.getEntity(user.entity())).isInstanceOf(UnknownIdentityException.class);
		forgetUser(user.entity());
		assertRejected(user, user.password());
		assertAccepted(control, control.password());
	}

	private void assertRejected(TestUser user, String password)
	{
		WebDriver browser = newBrowser();
		VaadinPage page = new VaadinPage(browser);
		browser.get(baseUrl + "/home");
		page.signIn(user.username(), password);
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(browser.getCurrentUrl()).contains("/home/authentication");
		assertThat(browser.findElements(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"))).isEmpty();
	}

	private void assertAccepted(TestUser user, String password)
	{
		VaadinPage page = new VaadinPage(newBrowser());
		page.login(baseUrl, "/home", user.username(), password);
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}
}
