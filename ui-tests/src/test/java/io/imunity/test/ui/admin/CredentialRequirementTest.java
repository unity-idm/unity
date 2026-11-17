package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;

@Tag("admin-authentication")
class CredentialRequirementTest extends UserAccountTestBase
{
	private final String name = "ui-requirement-" + UUID.randomUUID();

	@Autowired
	@Qualifier("insecure")
	private CredentialRequirementManagement requirements;
	@Autowired
	@Qualifier("insecure")
	private EntityCredentialManagement credentials;

	@AfterEach
	void removeRequirement() throws Exception
	{
		if (requirements.getCredentialRequirements().stream().anyMatch(requirement -> requirement.getName().equals(name)))
			requirements.removeCredentialRequirement(name, "ui-password");
	}

	@Test
	void shouldCreateEditAssignAndDeleteCredentialRequirement() throws Exception
	{
		TestUser user = createUser("Requirement member");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/credential-requirements/edit");
		page.fill(testId("CredentialRequirementsEditView.name"), name);
		page.fill(testId("CredentialRequirementsEditView.description"), "Initial requirement");
		page.selectMultiple(testId("CredentialRequirementsEditView.credentials"), "sys:password");
		save();
		driver.get(baseUrl + "/admin/credential-requirements/edit/" + name);
		assertThat(page.value(testId("CredentialRequirementsEditView.name"))).isEqualTo(name);
		assertThat(page.visible(testId("CredentialRequirementsEditView.credentials")).getText()).contains("sys:password");
		page.fill(testId("CredentialRequirementsEditView.description"), "Updated requirement");
		save();
		driver.get(baseUrl + "/admin/credential-requirements/edit/" + name);
		assertThat(page.value(testId("CredentialRequirementsEditView.description"))).isEqualTo("Updated requirement");
		CredentialRequirements stored = requirements.getCredentialRequirements(name);
		assertThat(stored.getDescription()).isEqualTo("Updated requirement");
		assertThat(stored.getRequiredCredentials()).containsExactly("sys:password");

		screenshot("Reloaded credential requirement");
		driver.get(baseUrl + "/admin/directory-browser");
		new DirectoryPage(driver).selectGroup("/");
		new AccountAdministrationPage(driver).changeRequirement(user.entity(), name);
		waitForElementNotPresent(testId("ChangeCredentialRequirementDialog"));
		assertThat(entities.getEntity(user.entity()).getCredentialInfo().getCredentialRequirementId()).isEqualTo(name);
		assertThat(entities.getEntity(user.entity()).getCredentialInfo().getCredentialsState()
				.get("sys:password").getState()).isEqualTo(LocalCredentialState.correct);
		VaadinPage home = new VaadinPage(newBrowser());
		home.login(baseUrl, "/home", user.username(), user.password());
		assertThat(home.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());

		credentials.setEntityCredentialRequirements(user.entity(), "ui-password");
		driver.get(baseUrl + "/admin/credential-requirements");
		new AdminPage(driver).deleteRequirement(name);
		waitForPageLoadByURL("/credential-requirements");
		assertThat(requirements.getCredentialRequirements()).extracting(CredentialRequirements::getName)
				.doesNotContain(name);
		assertThat(entities.getEntity(user.entity()).getCredentialInfo().getCredentialRequirementId())
				.isEqualTo("ui-password");
		driver.navigate().refresh();
		assertThat(page.visible(testId("CredentialRequirementsView.requirements")).getText()).doesNotContain(name);
	}

	@Test
	void shouldRejectUnnamedRequirementWithoutPersistingIt() throws Exception
	{
		int initialCount = requirements.getCredentialRequirements().size();
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/credential-requirements/edit");
		page.fill(testId("CredentialRequirementsEditView.description"), "Cannot save without a name");
		page.selectMultiple(testId("CredentialRequirementsEditView.credentials"), "sys:password");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitFor(() -> Boolean.parseBoolean(page.visible(testId("CredentialRequirementsEditView.name"))
				.getDomProperty("invalid")));
		assertThat(driver.getCurrentUrl()).endsWith("/credential-requirements/edit");
		assertThat(requirements.getCredentialRequirements()).hasSize(initialCount);
		screenshot("Missing credential requirement name");
	}

	private void save()
	{
		new VaadinPage(driver).click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/credential-requirements");
	}
}
